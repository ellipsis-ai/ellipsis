package controllers.api.v1

import controllers.{EllipsisController, RemoteAssets}
import java.time.OffsetDateTime
import javax.inject.Inject

import akka.actor.ActorSystem
import com.google.inject.Provider
import models.accounts.slack.botprofile.SlackBotProfile
import models.accounts.slack.profile.SlackProfile
import models.accounts.user.User
import models.behaviors.events._
import models.behaviors.invocationtoken.InvocationToken
import models.behaviors.scheduling.scheduledmessage.ScheduledMessage
import models.behaviors.{BotResultService, SimpleTextResult}
import models.team.Team
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.WSClient
import play.api.mvc.{AnyContent, Request, Result}
import play.api.{Configuration, Logger}
import services.caching.CacheService
import services.{AWSLambdaService, DataService, SlackEventService}
import utils.SlackTimestamp

import scala.concurrent.{ExecutionContext, Future}

class CommandsController @Inject() (
                                     override val configuration: Configuration,
                                     override val dataService: DataService,
                                     override val cacheService: CacheService,
                                     override val lambdaService: AWSLambdaService,
                                     override val ws: WSClient,
                                     override val slackService: SlackEventService,
                                     override val eventHandler: EventHandler,
                                     override val botResultService: BotResultService,
                                     override val assetsProvider: Provider[RemoteAssets],
                                     override implicit val actorSystem: ActorSystem,
                                     override implicit val ec: ExecutionContext
                                  )
  extends BaseController(configuration, dataService, cacheService, lambdaService, ws, slackService, eventHandler, botResultService, assetsProvider, actorSystem, ec) {

  class InvalidTokenException extends Exception

  trait ApiMethodInfo {
    val token: String
  }

  trait ApiMethodWithMessageInfo extends ApiMethodInfo {
    val message: String
  }

  case class ApiMethodContext(
                               maybeInvocationToken: Option[InvocationToken],
                               maybeUser: Option[User],
                               maybeBotProfile: Option[SlackBotProfile],
                               maybeSlackProfile: Option[SlackProfile],
                               maybeScheduledMessage: Option[ScheduledMessage],
                               maybeTeam: Option[Team],
                               isInvokedExternally: Boolean
                             ) {

    def maybeSlackChannelIdFor(channel: String): Future[Option[String]] = {
      maybeBotProfile.map { botProfile =>
        dataService.slackBotProfiles.channelsFor(botProfile, cacheService).maybeIdFor(channel)
      }.getOrElse(Future.successful(None))
    }

    def maybeBehaviorFor(actionName: String) = {
      for {
        maybeOriginatingBehavior <- maybeInvocationToken.map { invocationToken =>
          dataService.behaviors.findWithoutAccessCheck(invocationToken.behaviorId)
        }.getOrElse(Future.successful(None))
        maybeGroup <- Future.successful(maybeOriginatingBehavior.flatMap(_.maybeGroup))
        maybeBehavior <- maybeGroup.map { group =>
          dataService.behaviors.findByIdOrName(actionName, group)
        }.getOrElse(Future.successful(None))
      } yield maybeBehavior
    }

    def maybeMessageEventFor(message: String, channel: String, maybeOriginalEventType: Option[EventType]): Future[Option[Event]] = {
      maybeSlackChannelIdFor(channel).map { maybeSlackChannelId =>
        for {
          botProfile <- maybeBotProfile
          slackProfile <- maybeSlackProfile
        } yield {
          val slackEvent = SlackMessageEvent(
            botProfile,
            slackProfile.teamId,
            maybeSlackChannelId.getOrElse(channel),
            None,
            slackProfile.loginInfo.providerKey,
            SlackMessage.fromUnformattedText(message, botProfile.userId),
            None,
            SlackTimestamp.now,
            slackService.clientFor(botProfile),
            maybeOriginalEventType
          )
          val event: Event = maybeScheduledMessage.map { scheduledMessage =>
            ScheduledEvent(slackEvent, scheduledMessage)
          }.getOrElse(slackEvent)
          event
        }
      }
    }

  }

  object ApiMethodContext {

    private def maybeUserForApiToken(token: String): Future[Option[User]] = {
      for {
        maybeToken <- dataService.apiTokens.find(token)
        maybeValidToken <- maybeToken.map { token =>
          if (token.isValid) {
            dataService.apiTokens.use(token).map(_ => Some(token))
          } else {
            Future.successful(None)
          }
        }.getOrElse(Future.successful(None))

        maybeUser <- maybeValidToken.map { token =>
          dataService.users.find(token.userId)
        }.getOrElse(Future.successful(None))
      } yield maybeUser
    }

    def createFor(token: String): Future[ApiMethodContext] = {
      for {
        maybeUserForApiToken <- maybeUserForApiToken(token)
        maybeInvocationToken <- dataService.invocationTokens.findNotExpired(token)
        maybeScheduledMessage <- maybeInvocationToken.flatMap { token =>
          token.maybeScheduledMessageId.map { msgId =>
            dataService.scheduledMessages.find(msgId)
          }
        }.getOrElse(Future.successful(None))
        maybeUserForInvocationToken <- dataService.users.findForInvocationToken(token)
        maybeUser <- Future.successful(maybeUserForApiToken.orElse(maybeUserForInvocationToken))
        maybeTeam <- maybeUser.map { user =>
          dataService.teams.find(user.teamId)
        }.getOrElse {
          throw new InvalidTokenException()
        }
        maybeBotProfile <- maybeTeam.map { team =>
          dataService.slackBotProfiles.allFor(team).map(_.headOption)
        }.getOrElse(Future.successful(None))
        maybeSlackProfile <- maybeUser.map { user =>
          dataService.users.maybeSlackProfileFor(user)
        }.getOrElse(Future.successful(None))
      } yield {
        ApiMethodContext(
          maybeInvocationToken,
          maybeUser,
          maybeBotProfile,
          maybeSlackProfile,
          maybeScheduledMessage,
          maybeTeam,
          isInvokedExternally = maybeUserForApiToken.isDefined
        )
      }
    }

  }

  private def maybeIntroTextFor(event: Event, context: ApiMethodContext, isForInterruption: Boolean): Option[String] = {
    val greeting = if (isForInterruption) {
      "Meanwhile, "
    } else {
      s""":wave: Hi.
         |
       |""".stripMargin
    }
    if (context.isInvokedExternally) {
      Some(s"""${greeting}I’ve been asked to run `${event.messageText}`.
              |
       |───
              |""".stripMargin)
    } else {
      None
    }
  }

  private def runBehaviorFor(maybeEvent: Option[Event], context: ApiMethodContext) = {
    for {
      result <- maybeEvent.map { event =>
        for {
          result <- eventHandler.handle(event, None).map { results =>
            results.foreach { result =>
              val maybeIntro = maybeIntroTextFor(event, context, isForInterruption = false)
              val maybeInterruptionIntro = maybeIntroTextFor(event, context, isForInterruption = true)
              botResultService.sendIn(result, None, maybeIntro, maybeInterruptionIntro).map { _ =>
                Logger.info(event.logTextFor(result, Some("in response to /api/post_message")))
              }
            }
            Ok(Json.toJson(results.map(_.fullText)))
          }
        } yield result
      }.getOrElse {
        printApiContextError(context)
        Future.successful(InternalServerError("Request failed.\n"))
      }
    } yield result
  }

  case class RunActionArgumentInfo(name: String, value: String)

  trait ApiMethodWithActionInfo extends ApiMethodInfo {
    val actionName: Option[String]
    val trigger: Option[String]

  }

  trait ApiMethodWithActionAndArgumentsInfo extends ApiMethodWithActionInfo {
    val arguments: Seq[RunActionArgumentInfo]

    val argumentsMap: Map[String, String] = {
      arguments.map { ea =>
        (ea.name, ea.value)
      }.toMap
    }
  }

  private val actionNameAndTriggerError = "One and only one of actionName and trigger must be set"
  private def checkActionNameAndTrigger(info: ApiMethodWithActionInfo) = {
    (info.actionName.isDefined || info.trigger.isDefined) && (info.actionName.isEmpty || info.trigger.isEmpty)
  }

  private def resultForFormErrors[T <: ApiMethodInfo](formWithErrors: Form[T])(implicit r: Request[AnyContent]): Result = {
    badRequest(formWithErrors.errors.map(_.message).mkString(", "))
  }


  case class SayInfo(
                      message: String,
                      responseContext: String,
                      channel: String,
                      token: String,
                      originalEventType: Option[String]
                    ) extends ApiMethodWithMessageInfo

  implicit val sayInfoWrites = Json.writes[SayInfo]

  private val sayForm = Form(
    mapping(
      "message" -> nonEmptyText,
      "responseContext" -> nonEmptyText,
      "channel" -> nonEmptyText,
      "token" -> nonEmptyText,
      "originalEventType" -> optional(nonEmptyText)
    )(SayInfo.apply)(SayInfo.unapply)
  )

  def say = Action.async { implicit request =>
    sayForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(resultForFormErrors(formWithErrors))
      },
      info => {
        val eventualResult = for {
          context <- ApiMethodContext.createFor(info.token)
          maybeEvent <- context.maybeMessageEventFor(info.message, info.channel, EventType.maybeFrom(info.originalEventType))
          result <- maybeEvent.map { event =>
            val botResult = SimpleTextResult(event, None, info.message, forcePrivateResponse = false)
            botResultService.sendIn(botResult, None).map { _ =>
              Ok(Json.toJson(Seq(botResult.fullText)))
            }
          }.getOrElse {
            printApiContextError(context)
            Future.successful(InternalServerError("Request failed.\n"))
          }
        } yield result

        eventualResult.recover {
          case e: InvalidTokenException => badRequest("Invalid token\n", Json.toJson(info))
          case e: slack.api.ApiError => if (e.code == "channel_not_found") {
            badRequest(s"""Error: the channel "${info.channel}" could not be found.""" + "\n", Json.toJson(info))
          } else {
            // TODO: 400 seems like maybe the wrong kind of error here
            badRequest(s"Slack API error: ${e.code}\n", Json.toJson(info))
          }
        }
      }
    )

  }

  private def printApiContextError(context: ApiMethodContext): Unit = {
    Logger.error(
      s"""Event creation likely failed for API context:
         |
         |Slack bot profile ID: ${context.maybeBotProfile.map(_.userId).getOrElse("not found")}
         |Slack user profile ID: ${context.maybeSlackProfile.map(_.loginInfo.providerID).getOrElse("not found")}
         |""".stripMargin
    )
  }
}
