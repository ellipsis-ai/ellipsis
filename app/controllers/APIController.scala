package controllers

import javax.inject.Inject

import akka.actor.ActorSystem
import models.accounts.slack.botprofile.SlackBotProfile
import models.accounts.slack.profile.SlackProfile
import models.accounts.user.User
import models.behaviors.{BotResultService, SimpleTextResult}
import models.behaviors.events._
import models.behaviors.invocationtoken.InvocationToken
import models.behaviors.scheduling.scheduledmessage.ScheduledMessage
import models.team.Team
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.api.{Configuration, Logger}
import services.{AWSLambdaService, DataService, SlackEventService}
import utils.SlackTimestamp

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class APIController @Inject() (
                                val messagesApi: MessagesApi,
                                val configuration: Configuration,
                                val dataService: DataService,
                                val lambdaService: AWSLambdaService,
                                val ws: WSClient,
                                val slackService: SlackEventService,
                                val eventHandler: EventHandler,
                                val botResultService: BotResultService,
                                implicit val actorSystem: ActorSystem
                              )
  extends EllipsisController {

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
        dataService.slackBotProfiles.channelsFor(botProfile).maybeIdFor(channel)
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

    def maybeMessageEventFor(message: String, channel: String): Future[Option[Event]] = {
      maybeSlackChannelIdFor(channel).map { maybeSlackChannelId =>
        for {
          botProfile <- maybeBotProfile
          slackProfile <- maybeSlackProfile
        } yield {
          val slackEvent = SlackMessageEvent(
            botProfile,
            maybeSlackChannelId.getOrElse(channel),
            None,
            slackProfile.loginInfo.providerKey,
            message,
            SlackTimestamp.now,
            slackService.clientFor(botProfile)
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
        maybeSlackLinkedAccount <- maybeUser.map { user =>
          dataService.linkedAccounts.maybeForSlackFor(user)
        }.getOrElse(Future.successful(None))
        maybeSlackProfile <- maybeSlackLinkedAccount.map { slackLinkedAccount =>
          dataService.slackProfiles.find(slackLinkedAccount.loginInfo)
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
                val channelText = event.maybeChannel.map(c => s" in channel [$c]").getOrElse("")
                Logger.info(s"Sending result [${result.fullText}] in response to /api/post_message [${event.messageText}]$channelText")
              }
            }
            Ok(Json.toJson(results.map(_.fullText)))
          }
        } yield result
      }.getOrElse(Future.successful(NotFound("")))
    } yield result
  }

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

  private def resultForFormErrors[T <: ApiMethodWithActionInfo](formWithErrors: Form[T]): Result = {
    BadRequest(formWithErrors.errors.map(_.message).mkString(", "))
  }

  case class RunActionArgumentInfo(name: String, value: String)

  case class RunActionInfo(
                            actionName: Option[String],
                            trigger: Option[String],
                            arguments: Seq[RunActionArgumentInfo],
                            responseContext: String,
                            channel: String,
                            token: String
                          ) extends ApiMethodWithActionAndArgumentsInfo

  private val runActionForm = Form(
    mapping(
      "actionName" -> optional(nonEmptyText),
      "trigger" -> optional(nonEmptyText),
      "arguments" -> seq(
        mapping(
          "name" -> nonEmptyText,
          "value" -> nonEmptyText
        )(RunActionArgumentInfo.apply)(RunActionArgumentInfo.unapply)
      ),
      "responseContext" -> nonEmptyText,
      "channel" -> nonEmptyText,
      "token" -> nonEmptyText
    )(RunActionInfo.apply)(RunActionInfo.unapply) verifying(actionNameAndTriggerError, checkActionNameAndTrigger _)
  )

  private def runByName(
                         actionName: String,
                         info: RunActionInfo,
                         context: ApiMethodContext
                       )(implicit request: Request[AnyContent]): Future[Result] = {
    for {
      maybeSlackChannelId <- context.maybeSlackChannelIdFor(info.channel)
      maybeBehavior <- context.maybeBehaviorFor(actionName)
      maybeEvent <- Future.successful(
        for {
          botProfile <- context.maybeBotProfile
          slackProfile <- context.maybeSlackProfile
          behavior <- maybeBehavior
        } yield RunEvent(
          botProfile,
          behavior,
          info.argumentsMap,
          maybeSlackChannelId.getOrElse(info.channel),
          None,
          slackProfile.loginInfo.providerKey,
          SlackTimestamp.now,
          slackService.clientFor(botProfile)
        )
      )
      result <- runBehaviorFor(maybeEvent, context)
    } yield result
  }

  private def runByTrigger(
                         trigger: String,
                         info: RunActionInfo,
                         context: ApiMethodContext
                       )(implicit request: Request[AnyContent]): Future[Result] = {
    for {
      maybeEvent <- context.maybeMessageEventFor(trigger, info.channel)
      result <- runBehaviorFor(maybeEvent, context)
    } yield result
  }

  def runAction = Action.async { implicit request =>
    runActionForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(resultForFormErrors(formWithErrors))
      },
      info => {
        val eventualResult = for {
          context <- ApiMethodContext.createFor(info.token)
          result <- info.actionName.map { name =>
            runByName(name, info, context)
          }.getOrElse {
            info.trigger.map { trigger =>
              runByTrigger(trigger, info, context)
            }.getOrElse {
              Future.successful(BadRequest(actionNameAndTriggerError))
            }
          }
        } yield result

        eventualResult.recover {
          case e: InvalidTokenException => BadRequest("Invalid token")
        }
      }
    )

  }

  case class ScheduleActionInfo(
                                 actionName: Option[String],
                                 trigger: Option[String],
                                 arguments: Seq[RunActionArgumentInfo],
                                 recurrenceString: String,
                                 useDM: Boolean,
                                 channel: String,
                                 token: String
                                ) extends ApiMethodWithActionAndArgumentsInfo

  private val scheduleActionForm = Form(
    mapping(
      "actionName" -> optional(nonEmptyText),
      "trigger" -> optional(nonEmptyText),
      "arguments" -> seq(
        mapping(
          "name" -> nonEmptyText,
          "value" -> nonEmptyText
        )(RunActionArgumentInfo.apply)(RunActionArgumentInfo.unapply)
      ),
      "recurrence" -> nonEmptyText,
      "useDM" -> boolean,
      "channel" -> nonEmptyText,
      "token" -> nonEmptyText
    )(ScheduleActionInfo.apply)(ScheduleActionInfo.unapply) verifying(actionNameAndTriggerError, checkActionNameAndTrigger _)
  )

  private def scheduleByName(
                             actionName: String,
                             info: ScheduleActionInfo,
                             context: ApiMethodContext
                           )(implicit request: Request[AnyContent]): Future[Result] = {
    for {
      maybeSlackChannelId <- context.maybeSlackChannelIdFor(info.channel)
      maybeBehavior <- context.maybeBehaviorFor(actionName)
      result <- (for {
        slackProfile <- context.maybeSlackProfile
        behavior <- maybeBehavior
      } yield {
        for {
          user <- dataService.users.ensureUserFor(slackProfile.loginInfo, behavior.team.id)
          maybeScheduled <- dataService.scheduledBehaviors.maybeCreateWithRecurrenceText(
            behavior,
            info.argumentsMap,
            info.recurrenceString,
            user,
            behavior.team,
            maybeSlackChannelId,
            info.useDM
          )
          result <- maybeScheduled.map { scheduled =>
            scheduled.successResponse(dataService).map(Ok(_))
          }.getOrElse {
            Future.successful(BadRequest(s"Unable to schedule `$actionName` for `${info.recurrenceString}`"))
          }
        } yield result
      }).getOrElse(Future.successful(NotFound("Couldn't find an action to schedule")))
    } yield result
  }

  private def scheduleByTrigger(
                                trigger: String,
                                info: ScheduleActionInfo,
                                context: ApiMethodContext
                              )(implicit request: Request[AnyContent]): Future[Result] = {
    for {
      maybeSlackChannelId <- context.maybeSlackChannelIdFor(info.channel)
      maybeScheduled <- (for {
        team <- context.maybeTeam
        user <- context.maybeUser
      } yield {
        dataService.scheduledMessages.maybeCreateWithRecurrenceText(trigger, info.recurrenceString, user, team, maybeSlackChannelId, info.useDM)
      }).getOrElse(Future.successful(None))
      result <- maybeScheduled.map { scheduled =>
        scheduled.successResponse(dataService).map(Ok(_))
      }.getOrElse {
        Future.successful(BadRequest(s"Unable to schedule `$trigger` for `${info.recurrenceString}`"))
      }
    } yield result
  }

  def scheduleAction = Action.async { implicit request =>
    scheduleActionForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(resultForFormErrors(formWithErrors))
      },
      info => {
        val eventualResult = for {
          context <- ApiMethodContext.createFor(info.token)
          result <- info.actionName.map { actionName =>
            scheduleByName(actionName, info, context)
          }.getOrElse {
            info.trigger.map { trigger =>
              scheduleByTrigger(trigger, info, context)
            }.getOrElse {
              Future.successful(BadRequest(actionNameAndTriggerError))
            }
          }
        } yield result

        eventualResult.recover {
          case e: InvalidTokenException => BadRequest("Invalid token")
        }
      }
    )

  }

  case class UnscheduleActionInfo(
                                   actionName: Option[String],
                                   trigger: Option[String],
                                   userId: Option[String],
                                   channel: Option[String],
                                   token: String
                                 ) extends ApiMethodWithActionInfo

  private val unscheduleActionForm = Form(
    mapping(
      "actionName" -> optional(nonEmptyText),
      "trigger" -> optional(nonEmptyText),
      "userId" -> optional(nonEmptyText),
      "channel" -> optional(nonEmptyText),
      "token" -> nonEmptyText
    )(UnscheduleActionInfo.apply)(UnscheduleActionInfo.unapply) verifying(actionNameAndTriggerError, checkActionNameAndTrigger _)
  )

  private def unscheduleByName(
                                actionName: String,
                                info: UnscheduleActionInfo,
                                context: ApiMethodContext
                              )(implicit request: Request[AnyContent]): Future[Result] = {
    for {
      maybeBehavior <- context.maybeBehaviorFor(actionName)
      maybeUser <- info.userId.map { userId =>
        dataService.users.find(userId)
      }.getOrElse(Future.successful(None))
      result <- maybeBehavior.map { behavior =>
        if (info.userId.isDefined && maybeUser.isEmpty) {
          Future.successful(NotFound(s"Couldn't find a user with ID `${info.userId.get}`"))
        } else {
          dataService.scheduledBehaviors.allForBehavior(behavior, maybeUser, info.channel).flatMap { scheduledBehaviors =>
            if (scheduledBehaviors.isEmpty) {
              Future.successful(Ok("There was nothing to unschedule for this action"))
            } else {
              for {
                displayText <- scheduledBehaviors.head.displayText(dataService)
                _ <- Future.sequence(scheduledBehaviors.map { ea =>
                  dataService.scheduledBehaviors.delete(ea)
                })
              } yield {
                Ok(s"Ok, I unscheduled everything for $displayText")
              }
            }
          }
        }
      }.getOrElse(Future.successful(NotFound(s"Couldn't find an action with name `${info.actionName}`")))
    } yield result
  }

  private def unscheduleByTrigger(
                                   trigger: String,
                                   info: UnscheduleActionInfo,
                                   context: ApiMethodContext
                                  )(implicit request: Request[AnyContent]): Future[Result] = {
    context.maybeTeam.map { team =>
      dataService.scheduledMessages.deleteFor(trigger, team).map { didDelete =>
        if (didDelete) {
          Ok(s"Ok, I unscheduled everything for `$trigger`")
        } else {
          Ok("There was nothing to unschedule for this trigger")
        }
      }
    }.getOrElse {
      Future.successful(NotFound("Couldn't find team"))
    }

  }

  def unscheduleAction = Action.async { implicit request =>
    unscheduleActionForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(resultForFormErrors(formWithErrors))
      },
      info => {
        val eventualResult = for {
          context <- ApiMethodContext.createFor(info.token)
          result <- info.actionName.map { actionName =>
            unscheduleByName(actionName, info, context)
          }.getOrElse {
            info.trigger.map { trigger =>
              unscheduleByTrigger(trigger, info, context)
            }.getOrElse {
              Future.successful(BadRequest(actionNameAndTriggerError))
            }
          }
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
          context <- ApiMethodContext.createFor(info.token)
          maybeEvent <- context.maybeMessageEventFor(info.message, info.channel)
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
          context <- ApiMethodContext.createFor(info.token)
          maybeEvent <- context.maybeMessageEventFor(info.message, info.channel)
          result <- maybeEvent.map { event =>
            val botResult = SimpleTextResult(event, None, info.message, forcePrivateResponse = false)
            botResultService.sendIn(botResult, None).map { _ =>
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
