package controllers

import javax.inject.Inject

import akka.actor.ActorSystem
import models.accounts.slack.botprofile.SlackBotProfile
import models.accounts.slack.profile.SlackProfile
import models.accounts.user.User
import models.behaviors.SimpleTextResult
import models.behaviors.behavior.Behavior
import models.behaviors.events._
import models.behaviors.invocationtoken.InvocationToken
import models.behaviors.scheduling.scheduledmessage.ScheduledMessage
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

  trait ApiMethodWithMessageInfo extends ApiMethodInfo {
    val message: String
  }

  trait ApiMethodContextTrait {
    val maybeInvocationToken: Option[InvocationToken]
    val maybeUserForApiToken: Option[User]
    val maybeBotProfile: Option[SlackBotProfile]
    val maybeSlackProfile: Option[SlackProfile]
    val maybeScheduledMessage: Option[ScheduledMessage]
    val maybeSlackChannelId: Option[String]
  }

  case class ApiMethodContext(
                               maybeInvocationToken: Option[InvocationToken],
                               maybeUserForApiToken: Option[User],
                               maybeBotProfile: Option[SlackBotProfile],
                               maybeSlackProfile: Option[SlackProfile],
                               maybeScheduledMessage: Option[ScheduledMessage],
                               maybeSlackChannelId: Option[String]
                             ) extends ApiMethodContextTrait {

    def maybeEventFor(info: ApiMethodWithMessageInfo): Option[Event] = {
      maybeBotProfile.map { botProfile =>
        val slackEvent = SlackMessageEvent(
          botProfile,
          maybeSlackChannelId.getOrElse(info.channel),
          None,
          maybeSlackProfile.map(_.loginInfo.providerKey).getOrElse("api"),
          info.message,
          SlackTimestamp.now
        )
        val event: Event = maybeScheduledMessage.map { scheduledMessage =>
          ScheduledEvent(slackEvent, scheduledMessage)
        }.getOrElse(slackEvent)
        event
      }
    }

  }

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

  case class ApiMethodWithActionContext(
                                         maybeInvocationToken: Option[InvocationToken],
                                         maybeUserForApiToken: Option[User],
                                         maybeBotProfile: Option[SlackBotProfile],
                                         maybeSlackProfile: Option[SlackProfile],
                                         maybeScheduledMessage: Option[ScheduledMessage],
                                         maybeSlackChannelId: Option[String],
                                         maybeBehavior: Option[Behavior]
                                       ) extends ApiMethodContextTrait

  object ApiMethodWithActionContext {
    def apply(context: ApiMethodContext, maybeBehavior: Option[Behavior]): ApiMethodWithActionContext = {
      apply(
        context.maybeInvocationToken,
        context.maybeUserForApiToken,
        context.maybeBotProfile,
        context.maybeSlackProfile,
        context.maybeScheduledMessage,
        context.maybeSlackChannelId,
        maybeBehavior
      )
    }
  }

  def apiMethodWithActionContextFor(info: ApiMethodWithActionInfo): Future[ApiMethodWithActionContext] = {
    for {
      context <- apiMethodContextFor(info)
      maybeOriginatingBehavior <- context.maybeInvocationToken.map { invocationToken =>
        dataService.behaviors.findWithoutAccessCheck(invocationToken.behaviorId)
      }.getOrElse(Future.successful(None))
      maybeGroup <- Future.successful(maybeOriginatingBehavior.flatMap(_.maybeGroup))
      maybeBehavior <- maybeGroup.map { group =>
        dataService.behaviors.findByIdOrName(info.actionName, group)
      }.getOrElse(Future.successful(None))
    } yield {
      ApiMethodWithActionContext(context, maybeBehavior)
    }
  }

  private def runBehaviorFor(maybeEvent: Option[Event], context: ApiMethodContextTrait) = {
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
                  introResult.sendIn(None, None, dataService)
                }.getOrElse(Future.successful({}))
              } else {
                Future.successful({})
              }
              eventualIntroSend.flatMap { _ =>
                result.sendIn(None, None, dataService).map { _ =>
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

  trait ApiMethodWithActionInfo extends ApiMethodInfo {
    val actionName: String
    val params: Seq[RunActionParamInfo]

    val paramsMap: Map[String, String] = {
      params.map { ea =>
        (ea.name, ea.value)
      }.toMap
    }
  }

  case class RunActionParamInfo(name: String, value: String)

  case class RunActionInfo(
                            actionName: String,
                            params: Seq[RunActionParamInfo],
                            responseContext: String,
                            channel: String,
                            token: String
                          ) extends ApiMethodWithActionInfo

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
          context <- apiMethodWithActionContextFor(info)
          maybeEvent <- Future.successful(
            for {
              botProfile <- context.maybeBotProfile
              behavior <- context.maybeBehavior
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

  case class ScheduleActionInfo(
                                 actionName: String,
                                 params: Seq[RunActionParamInfo],
                                 recurrenceString: String,
                                 useDM: Boolean,
                                 responseContext: String,
                                 channel: String,
                                 token: String
                                ) extends ApiMethodWithActionInfo

  private val scheduleActionForm = Form(
    mapping(
      "actionName" -> nonEmptyText,
      "params" -> seq(
        mapping(
          "name" -> nonEmptyText,
          "value" -> nonEmptyText
        )(RunActionParamInfo.apply)(RunActionParamInfo.unapply)
      ),
      "recurrence" -> nonEmptyText,
      "useDM" -> boolean,
      "responseContext" -> nonEmptyText,
      "channel" -> nonEmptyText,
      "token" -> nonEmptyText
    )(ScheduleActionInfo.apply)(ScheduleActionInfo.unapply)
  )

  def scheduleAction = Action.async { implicit request =>
    scheduleActionForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.toString))
      },
      info => {
        val eventualResult = for {
          context <- apiMethodWithActionContextFor(info)
          result <- (for {
            slackProfile <- context.maybeSlackProfile
            behavior <- context.maybeBehavior
          } yield {
            for {
              user <- dataService.users.ensureUserFor(slackProfile.loginInfo, behavior.team.id)
              maybeScheduled <- dataService.scheduledBehaviors.maybeCreateFor(behavior, info.recurrenceString, user, behavior.team, context.maybeSlackChannelId, info.useDM)
            } yield {
              maybeScheduled.map { scheduled =>
                Ok(scheduled.successResponse)
              }.getOrElse {
                BadRequest(s"Unable to schedule for `${info.recurrenceString}`")
              }
            }
          }).getOrElse(Future.successful(NotFound(s"Couldn't find an action with name `${info.actionName}`")))
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
                            ) extends ApiMethodWithMessageInfo

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
                ScheduledEvent(slackEvent, scheduledMessage)
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

  case class SayInfo(
                      message: String,
                      responseContext: String,
                      channel: String,
                      token: String
                    ) extends ApiMethodWithMessageInfo

  private val sayForm = Form(
    mapping(
      "message" -> nonEmptyText,
      "responseContext" -> nonEmptyText,
      "channel" -> nonEmptyText,
      "token" -> nonEmptyText
    )(SayInfo.apply)(SayInfo.unapply)
  )

  def say = Action.async { implicit request =>
    sayForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.toString))
      },
      info => {
        val eventualResult = for {
          context <- apiMethodContextFor(info)
          maybeEvent <- Future.successful(context.maybeEventFor(info))
          result <- maybeEvent.map { event =>
            val botResult = SimpleTextResult(event, info.message, forcePrivateResponse = false)
            botResult.sendIn(None, None, dataService).map { _ =>
              Ok(Json.toJson(Seq(botResult.fullText)))
            }
          }.getOrElse(Future.successful(NotFound("")))
        } yield result

        eventualResult.recover {
          case e: InvalidTokenException => BadRequest("Invalid token")
        }
      }
    )

  }


}
