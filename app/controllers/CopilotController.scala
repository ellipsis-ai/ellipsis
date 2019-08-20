package controllers

import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

import akka.actor.ActorSystem
import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import javax.inject.Inject
import json.{InvocationLogEntryData, MessageListenerData}
import json.Formatting._
import models.accounts.slack.botprofile.SlackBotProfile
import models.accounts.{BotContext, BotProfile, MSTeamsContext, SlackContext}
import models.accounts.user.User
import models.behaviors.behaviorversion.Normal
import models.behaviors.SimpleTextResult
import models.behaviors.invocationlogentry.InvocationLogEntry
import models.behaviors.messagelistener.MessageListener
import models.silhouette.EllipsisEnv
import models.team.Team
import play.api.{Configuration, Logger}
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.mvc.{AnyContent, Request}
import play.filters.csrf.CSRF
import services.{DataService, DefaultServices}

import scala.concurrent.{ExecutionContext, Future}

class CopilotController @Inject()(
                                    val silhouette: Silhouette[EllipsisEnv],
                                    val configuration: Configuration,
                                    val services: DefaultServices,
                                    val ws: WSClient,
                                    val assetsProvider: Provider[RemoteAssets]
                                 )(
                                    implicit val actorSystem: ActorSystem,
                                    implicit val executor: ExecutionContext
                                  ) extends ReAuthable {

  val dataService: DataService = services.dataService

  case class CopilotConfig(containerId: String, csrfToken: Option[String])

  implicit lazy val copilotConfigFormat = Json.format[CopilotConfig]

  def index(maybeTeamId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    render.async {
      case Accepts.JavaScript() => {
        for {
          teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
        } yield {
          teamAccess.maybeTargetTeam.map { _ =>
            Ok(views.js.shared.webpackLoader(
              viewConfig(None), "CopilotConfig", "copilot", Json.toJson(CopilotConfig("copilot", CSRF.getToken(request).map(_.value)))
            ))
          }.getOrElse {
            NotFound("Team not found")
          }
        }
      }
      case Accepts.Html() => {
        for {
          teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
        } yield {
          teamAccess.maybeTargetTeam.map { _ =>
            val dataRoute = routes.CopilotController.index(maybeTeamId)
            Ok(views.html.copilot.index(viewConfig(Some(teamAccess)), dataRoute))
          }.getOrElse {
            NotFound("Team not found")
          }
        }
      }
    }
  }

  case class ResultsData(results: Seq[InvocationLogEntryData])

  implicit lazy val resultsDataFormat = Json.format[ResultsData]

  def resultsSince(when: String, maybeTeamId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    val since = try {
      OffsetDateTime.parse(when)
    } catch {
      case _: DateTimeParseException => OffsetDateTime.now.minusDays(1)
    }
    for {
      teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
      listeners <- dataService.messageListeners.allForUser(user)
      listenerData <- Future.sequence(listeners.map(ea => MessageListenerData.from(ea, teamAccess, dataService)))
      logEntries <- Future.sequence(listeners.map { ea =>
        dataService.invocationLogEntries.allForMessageListener(ea, since)
      }).map(_.flatten)
      resultsData <- Future.sequence(logEntries.map(ea => InvocationLogEntryData.fromEntryWithUserData(ea, services))).map(_.sortBy(_.createdAt))
    } yield {
      Ok(Json.toJson(ResultsData(resultsData)))
    }
  }

  def sendToChannel(invocationId: String) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    for {
      maybeInvocationEntry <- dataService.invocationLogEntries.findWithoutAccessCheck(invocationId)
      maybeListener <- maybeInvocationEntry.flatMap(_.maybeMessageListenerId.map { listenerId =>
        dataService.messageListeners.find(listenerId)
      }).getOrElse(Future.successful(None))
      maybeTeamAccess <- maybeInvocationEntry.map { entry =>
        dataService.users.teamAccessFor(user, Some(entry.behaviorVersion.team.id)).map(Some(_))
      }.getOrElse(Future.successful(None))
      result <- (for {
        entry <- maybeInvocationEntry
        listener <- maybeListener
        teamAccess <- maybeTeamAccess
        team <- teamAccess.maybeTargetTeam
      } yield {
        sendToChatFor(user, team, entry, listener)
      }).getOrElse {
        Future.successful(NotFound("Entry not found"))
      }
    } yield result
  }

  def sendToChatFor(user: User, team: Team, entry: InvocationLogEntry, listener: MessageListener)(implicit request: Request[AnyContent]) = {
    val channel = listener.channel
    for {
      userData <- dataService.users.userDataFor(user, team)
      maybeBotProfile <- BotContext.maybeContextFor(entry.context) match {
        case Some(SlackContext) => dataService.slackBotProfiles.maybeFirstFor(team, user)
        case Some(MSTeamsContext) => dataService.msTeamsBotProfiles.allFor(team.id).map(_.headOption)
        case _ => Future.successful(None)
      }
      resultTextToSend <- Future.successful {
        s"""${userData.fullName.getOrElse("Someone")} asked me to send a response to:
           |
           |> ${entry.messageText}
           |
           |${entry.resultText}
           |""".stripMargin
      }
      maybePermalink <- maybeBotProfile.map {
        case slackBotProfile: SlackBotProfile => for {
          maybeTs <- dataService.slackBotProfiles.sendResultWithNewEvent(
            "Copilot result sent to chat",
            (event) => Future.successful(Some(SimpleTextResult(event, None, resultTextToSend, Normal))),
            slackBotProfile,
            channel,
            user.id,
            entry.createdAt.toString,
            entry.maybeOriginalEventType,
            listener.maybeThreadId,
            isEphemeral = false,
            maybeResponseUrl = None,
            beQuiet = false
          )
          maybePermalink <- maybeTs.map { ts =>
            services.slackApiService.clientFor(slackBotProfile).permalinkFor(channel, ts)
          }.getOrElse(Future.successful(None))
        } yield maybePermalink
        case o => {
          Logger.error(s"Sending a copilot result to chat has not been implemented for ${o}")
          Future.successful(None)
        }
      }.getOrElse(Future.successful(None))
    } yield {
      maybePermalink.map { permalink =>
        Ok(Json.toJson(permalink))
      }.getOrElse {
        BadRequest("Channel not found")
      }
    }
  }
}
