package controllers

import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

import akka.actor.ActorSystem
import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import javax.inject.Inject
import json.{InvocationLogEntryData, MessageListenerData, UserData}
import json.Formatting._
import models.accounts.slack.botprofile.SlackBotProfile
import models.accounts.user.User
import models.accounts.{BotContext, SlackContext}
import models.behaviors.{BotResult, SimpleTextResult, SuccessResult}
import models.behaviors.behaviorversion.Normal
import models.behaviors.events.Event
import models.behaviors.invocationlogentry.InvocationLogEntry
import models.behaviors.messagelistener.MessageListener
import models.silhouette.EllipsisEnv
import models.team.Team
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.mvc.{AnyContent, Request, Result}
import play.api.{Configuration, Logger}
import play.filters.csrf.CSRF
import services.caching.SuccessResultData
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

  case class CopilotConfig(containerId: String, csrfToken: Option[String], listener: MessageListenerData)

  implicit lazy val copilotConfigFormat = Json.format[CopilotConfig]

  def index(listenerId: String) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    for {
      maybeListener <- dataService.messageListeners.find(listenerId, user)
      teamAccess <- dataService.users.teamAccessFor(user, maybeListener.map(_.behavior.team.id))
      maybeListenerData <- maybeListener.map { listener =>
        MessageListenerData.from(listener, teamAccess.loggedInTeam, services).map(Some(_))
      }.getOrElse(Future.successful(None))
    } yield {
      render {
        case Accepts.JavaScript() => {
          maybeListenerData.map { listenerData =>
            val config = CopilotConfig(
              "copilot",
              CSRF.getToken(request).map(_.value),
              listenerData
            )
            Ok(views.js.shared.webpackLoader(
              viewConfig(None), "CopilotConfig", "copilot", Json.toJson(config)
            ))
          }.getOrElse {
            NotFound("Copilot not found")
          }
        }
        case Accepts.Html() => {
          maybeListenerData.map { _ =>
            val dataRoute = routes.CopilotController.index(listenerId)
            Ok(views.html.copilot.index(viewConfig(Some(teamAccess)), dataRoute))
          }.getOrElse {
            NotFound("Copilot not found")
          }
        }
      }
    }
  }

  case class ResultsData(results: Seq[InvocationLogEntryData])

  implicit lazy val resultsDataFormat = Json.format[ResultsData]

  def resultsSince(listenerId: String, when: String) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    val since = try {
      OffsetDateTime.parse(when)
    } catch {
      case _: DateTimeParseException => OffsetDateTime.now.minusHours(MessageListener.COPILOT_EXPIRY_IN_HOURS)
    }
    for {
      maybeListener <- dataService.messageListeners.find(listenerId, user)
      _ <- maybeListener.map { listener =>
        dataService.messageListeners.noteCopilotActivity(listener)
      }.getOrElse(Future.successful({}))
      logEntries <- maybeListener.map { listener =>
        dataService.invocationLogEntries.allForMessageListener(listener, since)
      }.getOrElse(Future.successful(Seq()))
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
        dataService.messageListeners.find(listenerId, user)
      }).getOrElse(Future.successful(None))
      maybeTeamAccess <- maybeInvocationEntry.map { entry =>
        dataService.users.teamAccessFor(user, Some(entry.behaviorVersion.team.id)).map(Some(_))
      }.getOrElse(Future.successful(None))
      result <- (for {
        entry <- maybeInvocationEntry
        listener <- maybeListener
      } yield {
        sendToChatFor(user, listener.behavior.team, entry, listener, maybeTeamAccess.exists(_.isAdminAccess))
      }).getOrElse {
        Future.successful(NotFound("Entry not found"))
      }
    } yield result
  }

  private def sendToChatFor(user: User, team: Team, entry: InvocationLogEntry, listener: MessageListener, isAdminAccess: Boolean)
                           (implicit request: Request[AnyContent]): Future[Result] = {
    val channel = listener.channel
    for {
      userData <- dataService.users.userDataFor(user, team)
      maybeBotProfile <- BotContext.maybeContextFor(entry.context) match {
        case Some(SlackContext) => dataService.slackBotProfiles.maybeFirstFor(team, user)
// Todo: Implement MS Teams copilot functionality
//        case Some(MSTeamsContext) => dataService.msTeamsBotProfiles.allFor(team.id).map(_.headOption)
        case _ => {
          Logger.error(s"Sending to chat not implemented for ${entry.context}")
          Future.successful(None)
        }
      }
      maybeResult <- SuccessResultData.maybeSuccessResultFor(entry, dataService, services.cacheService)
      maybeOriginalPermalink <- maybeResult.map(_.event.maybePermalinkFor(services)).getOrElse(Future.successful(None))
      maybePermalink <- maybeBotProfile.map {
        case slackBotProfile: SlackBotProfile => for {
          maybeTs <- {
            val maybeSlackUserId = userData.userIdForContext.orElse {
              if (isAdminAccess) {
                Some(slackBotProfile.userId)
              } else {
                None
              }
            }
            maybeSlackUserId.map { slackUserId =>
              dataService.slackBotProfiles.sendResultWithNewEvent(
                "Copilot result sent to chat",
                (event) => Future
                  .successful(maybeOverrideResultFor(event, userData, entry, maybeResult, maybeOriginalPermalink)),
                slackBotProfile,
                channel,
                slackUserId,
                maybeResult.flatMap(_.event.maybeMessageId).getOrElse(entry.createdAt.toString),
                entry.maybeOriginalEventType,
                maybeResult.flatMap(_.event.maybeThreadId).orElse(listener.maybeThreadId),
                isEphemeral = false,
                maybeResponseUrl = None,
                beQuiet = false
              )
            }.getOrElse {
              Logger.error(
                s"""Tried to send a copilot result but no Slack user ID was found:
                   | - Ellipsis user ID ${userData.ellipsisUserId} (${userData.toString})
                   | - Ellipsis team ${team.name} (ID ${team.id})
                   | - Slack team ID ${slackBotProfile.slackTeamId}
                   |""".stripMargin
              )
              Future.successful(None)
            }
          }
          maybePermalink <- maybeTs.map { ts =>
            services.slackApiService.clientFor(slackBotProfile).permalinkFor(channel, ts)
          }.getOrElse(Future.successful(None))
        } yield maybePermalink
        case _ => Future.successful(None)
      }.getOrElse(Future.successful(None))
    } yield {
      maybePermalink.map { permalink =>
        Ok(Json.toJson(permalink))
      }.getOrElse {
        BadRequest("Channel not found")
      }
    }
  }

  private def maybeOverrideResultFor(
                                      event: Event,
                                      userData: UserData,
                                      entry: InvocationLogEntry,
                                      maybeResult: Option[SuccessResult],
                                      maybeOriginalPermalink: Option[String]
                                    ): Option[BotResult] = {
    val fallbackOriginal = s"\n> ${entry.messageText.replaceAll("\\n", "\n> ")}"
    val prefix = (userData.formattedLink.map { name =>
      maybeOriginalPermalink.map { permalink =>
        s"$name asked me to send a response to [an earlier message]($permalink):"
      }.getOrElse {
        s"$name asked me to send a response to an earlier message: $fallbackOriginal"
      }
    }.getOrElse {
      maybeOriginalPermalink.map { permalink =>
        s"I’ve been asked to send a response to [an earlier message]($permalink):"
      }.getOrElse {
        s"I’ve been asked to send a response to an earlier message: $fallbackOriginal"
      }
    }) + "\n\n"
    maybeResult.map { original =>
      original.copy(
        isForCopilot = false,
        maybeResponseTemplate = original.maybeResponseTemplate.map { rt =>
          prefix + rt
        }.orElse(Some(prefix))
      )
    }.orElse {
      val resultText = prefix + entry.resultText
      Some(SimpleTextResult(event, None, resultText, Normal))
    }
  }
}
