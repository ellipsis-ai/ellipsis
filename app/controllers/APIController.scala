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

  trait ApiMethodInfo {
    val responseContext: String
    val channel: String
    val token: String
  }

  trait ApiMethodWithMessageInfo extends ApiMethodInfo {
    val message: String
  }

  trait ApiMethodContext {
    val maybeInvocationToken: Option[InvocationToken]
    val maybeUserForApiToken: Option[User]
    val maybeBotProfile: Option[SlackBotProfile]
    val maybeSlackProfile: Option[SlackProfile]
    val maybeScheduledMessage: Option[ScheduledMessage]
  }

  case class BaseApiMethodContext(
                               maybeInvocationToken: Option[InvocationToken],
                               maybeUserForApiToken: Option[User],
                               maybeBotProfile: Option[SlackBotProfile],
                               maybeSlackProfile: Option[SlackProfile],
                               maybeScheduledMessage: Option[ScheduledMessage]
                             ) extends ApiMethodContext

  object BaseApiMethodContext {

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

    def createFor(info: ApiMethodInfo): Future[BaseApiMethodContext] = {
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
      } yield {
        BaseApiMethodContext(maybeInvocationToken, maybeUserForApiToken, maybeBotProfile, maybeSlackProfile, maybeScheduledMessage)
      }
    }

  }

  case class ApiMethodWithChannelContext(
                                         maybeInvocationToken: Option[InvocationToken],
                                         maybeUserForApiToken: Option[User],
                                         maybeBotProfile: Option[SlackBotProfile],
                                         maybeSlackProfile: Option[SlackProfile],
                                         maybeScheduledMessage: Option[ScheduledMessage],
                                         maybeSlackChannelId: Option[String]
                                       ) extends ApiMethodContext {

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

  object ApiMethodWithChannelContext {

    def createFor(info: ApiMethodInfo, channel: String): Future[ApiMethodWithChannelContext] = {
      for {
        baseContext <- BaseApiMethodContext.createFor(info)
        context <- baseContext.maybeBotProfile.map { botProfile =>
          dataService.slackBotProfiles.channelsFor(botProfile).maybeIdFor(channel)
        }.getOrElse(Future.successful(None)).map { maybeSlackChannelId =>
          ApiMethodWithChannelContext(
            baseContext.maybeInvocationToken,
            baseContext.maybeUserForApiToken,
            baseContext.maybeBotProfile,
            baseContext.maybeSlackProfile,
            baseContext.maybeScheduledMessage,
            maybeSlackChannelId
          )
        }
      } yield context
    }

    def createFor(info: ApiMethodWithMessageInfo): Future[ApiMethodWithChannelContext] = {
      createFor(info, info.channel)
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
                                       ) extends ApiMethodContext

  object ApiMethodWithActionContext {

    def apply(context: ApiMethodWithChannelContext, maybeBehavior: Option[Behavior]): ApiMethodWithActionContext = {
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

    def createFor(info: ApiMethodWithActionInfo): Future[ApiMethodWithActionContext] = {
      for {
        context <- ApiMethodWithChannelContext.createFor(info, info.channel)
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
    val arguments: Seq[RunActionArgumentInfo]

    val argumentsMap: Map[String, String] = {
      arguments.map { ea =>
        (ea.name, ea.value)
      }.toMap
    }
  }

  case class RunActionArgumentInfo(name: String, value: String)

  case class RunActionInfo(
                            actionName: String,
                            arguments: Seq[RunActionArgumentInfo],
                            responseContext: String,
                            channel: String,
                            token: String
                          ) extends ApiMethodWithActionInfo

  private val runActionForm = Form(
    mapping(
      "actionName" -> nonEmptyText,
      "arguments" -> seq(
        mapping(
          "name" -> nonEmptyText,
          "value" -> nonEmptyText
        )(RunActionArgumentInfo.apply)(RunActionArgumentInfo.unapply)
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
          context <- ApiMethodWithActionContext.createFor(info)
          maybeEvent <- Future.successful(
            for {
              botProfile <- context.maybeBotProfile
              behavior <- context.maybeBehavior
            } yield RunEvent(
              botProfile,
              behavior,
              info.argumentsMap,
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
                                 arguments: Seq[RunActionArgumentInfo],
                                 recurrenceString: String,
                                 useDM: Boolean,
                                 responseContext: String,
                                 channel: String,
                                 token: String
                                ) extends ApiMethodWithActionInfo

  private val scheduleActionForm = Form(
    mapping(
      "actionName" -> nonEmptyText,
      "arguments" -> seq(
        mapping(
          "name" -> nonEmptyText,
          "value" -> nonEmptyText
        )(RunActionArgumentInfo.apply)(RunActionArgumentInfo.unapply)
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
          context <- ApiMethodWithActionContext.createFor(info)
          result <- (for {
            slackProfile <- context.maybeSlackProfile
            behavior <- context.maybeBehavior
          } yield {
            for {
              user <- dataService.users.ensureUserFor(slackProfile.loginInfo, behavior.team.id)
              maybeScheduled <- dataService.scheduledBehaviors.maybeCreateFor(
                behavior,
                info.argumentsMap,
                info.recurrenceString,
                user,
                behavior.team,
                context.maybeSlackChannelId,
                info.useDM
              )
              result <- maybeScheduled.map { scheduled =>
                scheduled.successResponse(dataService).map(Ok(_))
              }.getOrElse {
                Future.successful(BadRequest(s"Unable to schedule for `${info.recurrenceString}`"))
              }
            } yield result
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
          context <- ApiMethodWithChannelContext.createFor(info)
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
          context <- ApiMethodWithChannelContext.createFor(info)
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
