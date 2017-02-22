package controllers

import javax.inject.Inject

import akka.actor.ActorSystem
import models.accounts.slack.botprofile.SlackBotProfile
import models.accounts.slack.profile.SlackProfile
import models.accounts.user.User
import models.behaviors.SimpleTextResult
import models.behaviors.events._
import models.behaviors.invocationtoken.InvocationToken
import models.behaviors.scheduledmessage.ScheduledMessage
import play.api.cache.CacheApi
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.Action
import play.api.{Configuration, Logger}
import services.{DataService, SlackEventService}
import utils.SlackTimestamp

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class APIController @Inject() (
                                val messagesApi: MessagesApi,
                                val configuration: Configuration,
                                val dataService: DataService,
                                val ws: WSClient,
                                val cache: CacheApi,
                                val slackService: SlackEventService,
                                val eventHandler: EventHandler,
                                implicit val actorSystem: ActorSystem
                              )
  extends EllipsisController {

  class InvalidTokenException extends Exception

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

  trait ApiMethodInfo {
    val responseContext: String
    val channel: String
    val token: String
  }

  case class ApiMethodContext(
                               maybeInvocationToken: Option[InvocationToken],
                               maybeUserForApiToken: Option[User],
                               maybeBotProfile: Option[SlackBotProfile],
                               maybeSlackProfile: Option[SlackProfile],
                               maybeScheduledMessage: Option[ScheduledMessage],
                               maybeSlackChannelId: Option[String]
                             )

  def apiMethodContextFor(info: ApiMethodInfo): Future[ApiMethodContext] = {
    for {
      maybeUserForApiToken <- maybeUserForApiToken(info.token)
      maybeInvocationToken <- dataService.invocationTokens.findNotExpired(info.token)
      maybeScheduledMessage <- maybeInvocationToken.flatMap { token =>
        token.maybeScheduledMessageId.map { msgId =>
          dataService.scheduledMessages.find(msgId)
        }
      }.getOrElse(Future.successful(None))
      maybeUserForInvocationToken <- dataService.users.findForInvocationToken(info.token)
      maybeUser <- Future.successful(maybeUserForApiToken.orElse(maybeUserForInvocationToken))
      maybeTeam <- maybeUser.map { user =>
        dataService.teams.find(user.teamId)
      }.getOrElse {
        throw new InvalidTokenException()
      }
      maybeBotProfile <- maybeTeam.map { team =>
        dataService.slackBotProfiles.allFor(team).map(_.headOption)
      }.getOrElse(Future.successful(None))
      maybeSlackLinkedAccount <- maybeUser.map { user =>
        dataService.linkedAccounts.maybeForSlackFor(user)
      }.getOrElse(Future.successful(None))
      maybeSlackProfile <- maybeSlackLinkedAccount.map { slackLinkedAccount =>
        dataService.slackProfiles.find(slackLinkedAccount.loginInfo)
      }.getOrElse(Future.successful(None))
      maybeSlackChannelId <- maybeBotProfile.map { botProfile =>
        dataService.slackBotProfiles.channelsFor(botProfile).maybeIdFor(info.channel)
      }.getOrElse(Future.successful(None))
    } yield {
      ApiMethodContext(maybeInvocationToken, maybeUserForApiToken, maybeBotProfile, maybeSlackProfile, maybeScheduledMessage, maybeSlackChannelId)
    }
  }

  private def runBehaviorFor(maybeEvent: Option[Event], context: ApiMethodContext) = {
    for {
      isInvokedExternally <- Future.successful(context.maybeUserForApiToken.isDefined)
      result <- maybeEvent.map { event =>
        for {
          didInterrupt <- eventHandler.interruptOngoingConversationsFor(event)
          result <- eventHandler.handle(event, None).map { results =>
            results.foreach { result =>
              val eventualIntroSend = if (isInvokedExternally) {
                context.maybeSlackProfile.map { slackProfile =>
                  val resultText = if (didInterrupt) {
                    s"""Meanwhile, <@${slackProfile.loginInfo.providerKey}> asked me to run `${event.messageText}`.
                       |
                             |───
                       |""".stripMargin
                  } else {
                    s""":wave: Hi.
                       |
                             |<@${slackProfile.loginInfo.providerKey}> asked me to run `${event.messageText}`.
                       |
                             |───
                       |""".stripMargin
                  }
                  val introResult = SimpleTextResult(event, resultText, result.forcePrivateResponse)
                  introResult.sendIn(None, None)
                }.getOrElse(Future.successful({}))
              } else {
                Future.successful({})
              }
              eventualIntroSend.flatMap { _ =>
                result.sendIn(None, None).map { _ =>
                  val channelText = event.maybeChannel.map(c => s" in channel [$c]").getOrElse("")
                  Logger.info(s"Sending result [${result.fullText}] in response to /api/post_message [${event.messageText}]$channelText")
                }
              }
            }
            Ok(Json.toJson(results.map(_.fullText)))
          }
        } yield result
      }.getOrElse(Future.successful(NotFound("")))
    } yield result
  }

  case class RunActionParamInfo(name: String, value: String)

  case class RunActionInfo(
                            actionName: String,
                            params: Seq[RunActionParamInfo],
                            responseContext: String,
                            channel: String,
                            token: String
                          ) extends ApiMethodInfo {

    val paramsMap: Map[String, String] = {
      params.map { ea =>
        (ea.name, ea.value)
      }.toMap
    }
  }

  private val runActionForm = Form(
    mapping(
      "actionName" -> nonEmptyText,
      "params" -> seq(
        mapping(
          "name" -> nonEmptyText,
          "value" -> nonEmptyText
        )(RunActionParamInfo.apply)(RunActionParamInfo.unapply)
      ),
      "responseContext" -> nonEmptyText,
      "channel" -> nonEmptyText,
      "token" -> nonEmptyText
    )(RunActionInfo.apply)(RunActionInfo.unapply)
  )

  def runAction = Action.async { implicit request =>
    runActionForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.toString))
      },
      info => {
        val eventualResult = for {
          context <- apiMethodContextFor(info)
          maybeOriginatingBehavior <- context.maybeInvocationToken.map { invocationToken =>
            dataService.behaviors.findWithoutAccessCheck(invocationToken.behaviorId)
          }.getOrElse(Future.successful(None))
          maybeGroup <- Future.successful(maybeOriginatingBehavior.flatMap(_.maybeGroup))
          maybeBehavior <- maybeGroup.map { group =>
            dataService.behaviors.findByIdOrName(info.actionName, group)
          }.getOrElse(Future.successful(None))
          maybeEvent <- Future.successful(
            for {
              botProfile <- context.maybeBotProfile
              behavior <- maybeBehavior
            } yield RunEvent(
              botProfile,
              behavior,
              info.paramsMap,
              context.maybeSlackChannelId.getOrElse(info.channel),
              None,
              context.maybeSlackProfile.map(_.loginInfo.providerKey).getOrElse("api"),
              SlackTimestamp.now
            )
          )
          result <- runBehaviorFor(maybeEvent, context)
        } yield result

        eventualResult.recover {
          case e: InvalidTokenException => BadRequest("Invalid token")
        }
      }
    )

  }

  case class PostMessageInfo(
                              message: String,
                              responseContext: String,
                              channel: String,
                              token: String
                            ) extends ApiMethodInfo

  private val postMessageForm = Form(
    mapping(
      "message" -> nonEmptyText,
      "responseContext" -> nonEmptyText,
      "channel" -> nonEmptyText,
      "token" -> nonEmptyText
    )(PostMessageInfo.apply)(PostMessageInfo.unapply)
  )

  def postMessage = Action.async { implicit request =>
    postMessageForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.toString))
      },
      info => {
        val eventualResult = for {
          context <- apiMethodContextFor(info)
          maybeEvent <- Future.successful(
            context.maybeBotProfile.map { botProfile =>
              val slackEvent = SlackMessageEvent(
                botProfile,
                context.maybeSlackChannelId.getOrElse(info.channel),
                None,
                context.maybeSlackProfile.map(_.loginInfo.providerKey).getOrElse("api"),
                info.message,
                SlackTimestamp.now
              )
              val event: Event = context.maybeScheduledMessage.map { scheduledMessage =>
                ScheduledMessageEvent(slackEvent, scheduledMessage)
              }.getOrElse(slackEvent)
              event
            }
          )
          result <- runBehaviorFor(maybeEvent, context)
        } yield result

        eventualResult.recover {
          case e: InvalidTokenException => BadRequest("Invalid token")
        }
      }
    )

  }


}
