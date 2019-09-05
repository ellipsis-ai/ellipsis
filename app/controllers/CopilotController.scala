package controllers

import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

import akka.actor.ActorSystem
import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.SecuredRequest
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

  case class CopilotConfig(
                            containerId: String,
                            csrfToken: Option[String],
                            teamName: String,
                            listener: MessageListenerData
                          )

  implicit lazy val copilotConfigFormat = Json.format[CopilotConfig]

  def index(listenerId: String) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    for {
      maybeListener <- dataService.messageListeners.find(listenerId, user)
      teamAccess <- dataService.users.teamAccessFor(user, maybeListener.map(_.behavior.team.id))
      maybeListenerData <- maybeListener.map { listener =>
        MessageListenerData.from(listener, services).map(Some(_))
      }.getOrElse(Future.successful(None))
    } yield {
      render {
        case Accepts.JavaScript() => {
          maybeListenerData.map { listenerData =>
            val config = CopilotConfig(
              "copilot",
              CSRF.getToken(request).map(_.value),
              teamAccess.targetTeamName,
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
      resultsData <- Future.sequence(logEntries.map(ea => InvocationLogEntryData.withData(ea, services)))
        .map(_.sortBy(_.createdAt))
    } yield {
      Ok(Json.toJson(ResultsData(resultsData)))
    }
  }

  case class SendToChannelOptions(text: Option[String])

  private implicit val sendToChannelOptionsRead = Json.reads[SendToChannelOptions]

  def sendToChannel(invocationId: String) = silhouette.SecuredAction(parse.json).async { implicit request =>
    val user = request.identity
    request.body.validate[SendToChannelOptions].fold(
      jsonError => Future.successful(BadRequest(JsError.toJson(jsonError))),
      options => {
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
            sendToChatFor(
              user,
              listener.behavior.team,
              entry,
              listener,
              maybeTeamAccess.exists(_.isAdminAccess),
              options)
          }).getOrElse {
            Future.successful(NotFound("Entry not found"))
          }
        } yield result
      }
    )
  }

  private def sendToChatFor(
                             user: User,
                             team: Team,
                             entry: InvocationLogEntry,
                             listener: MessageListener,
                             isAdminAccess: Boolean,
                             options: SendToChannelOptions
                           )(implicit request: SecuredRequest[EllipsisEnv, JsValue]): Future[Result] = {
    val channel = listener.channel
    for {
      copilotUserData <- dataService.users.userDataFor(user, team)
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
      originalAuthorData <- dataService.users.userDataFor(entry.user, team)
      maybeOriginalPermalink <- maybeResult.map(_.event.maybePermalinkFor(services)).getOrElse(Future.successful(None))
      maybePermalink <- maybeBotProfile.map {
        case slackBotProfile: SlackBotProfile => for {
          maybeTs <- {
            val maybeSlackUserId = copilotUserData.userIdForContext.orElse {
              if (isAdminAccess) {
                Some(slackBotProfile.userId)
              } else {
                None
              }
            }
            maybeSlackUserId.map { slackUserId =>
              dataService.slackBotProfiles.sendResultWithNewEvent(
                "Copilot result sent to chat",
                (event) => Future.successful(
                  maybeOverrideResultFor(
                    event,
                    copilotUserData,
                    entry,
                    originalAuthorData,
                    maybeResult,
                    maybeOriginalPermalink,
                    options.text)
                ),
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
                   | - Ellipsis user ID ${copilotUserData.ellipsisUserId} (${copilotUserData.toString})
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
                                      copilotUserData: UserData,
                                      entry: InvocationLogEntry,
                                      originalAuthorUserData: UserData,
                                      maybeResult: Option[SuccessResult],
                                      maybeOriginalPermalink: Option[String],
                                      maybeReplacementText: Option[String]
                                    ): Option[BotResult] = {
    val fallbackOriginal = s"\n> ${entry.messageText.replaceAll("\\n", "\n> ")}"
    val originalAuthor = originalAuthorUserData.formattedLink
      .filter(original => !copilotUserData.formattedLink.contains(original)).map(original => s" from ${original}")
      .getOrElse("")
    val prefix = (copilotUserData.formattedLink.map { name =>
      maybeOriginalPermalink.map { permalink =>
        s"$name asked me to send a response to [an earlier message]($permalink)${originalAuthor}:"
      }.getOrElse {
        s"$name asked me to send a response to an earlier message${originalAuthor}: $fallbackOriginal"
      }
    }.getOrElse {
      maybeOriginalPermalink.map { permalink =>
        s"I’ve been asked to send a response to [an earlier message]($permalink)${originalAuthor}:"
      }.getOrElse {
        s"I’ve been asked to send a response to an earlier message${originalAuthor}: $fallbackOriginal"
      }
    }) + "\n\n"
    val text = prefix + maybeReplacementText.getOrElse(entry.resultText)
    maybeResult.map(_.copy(
      isForCopilot = false,
      maybeResponseTemplate = Some(text)
    )).orElse(
      Some(SimpleTextResult(event, None, text, Normal))
    )
  }
}
