package controllers

import java.time.OffsetDateTime

import javax.inject.Inject
import akka.actor.ActorSystem
import com.google.inject.Provider
import json.{APIErrorData, APIResultWithErrorsData, APITokenData, UserData}
import json.Formatting._
import models.accounts.slack.botprofile.SlackBotProfile
import models.accounts.slack.profile.SlackProfile
import models.accounts.user.User
import models.behaviors.behaviorversion.{BehaviorVersion, Normal}
import models.behaviors.events._
import models.behaviors.invocationtoken.InvocationToken
import models.behaviors.scheduling.scheduledmessage.ScheduledMessage
import models.behaviors.{BotResult, BotResultService, SimpleTextResult}
import models.team.Team
import play.api.data.{Form, FormError}
import play.api.data.Forms._
import play.api.http.HttpEntity
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.mvc.{AnyContent, Request, Result}
import play.api.{Configuration, Logger}
import services.caching.CacheService
import services.slack.{SlackApiError, SlackEventService}
import services.{AWSLambdaService, DataService, DefaultServices}
import utils.{SlackFileMap, SlackMessageSenderChannelException, SlackMessageSenderException, SlackTimestamp}

import scala.concurrent.{ExecutionContext, Future}

class APIController @Inject() (
                                val services: DefaultServices,
                                val eventHandler: EventHandler,
                                val assetsProvider: Provider[RemoteAssets],
                                implicit val actorSystem: ActorSystem,
                                implicit val ec: ExecutionContext
                              )
  extends EllipsisController {

  val ws: WSClient = services.ws
  val configuration: Configuration = services.configuration
  val dataService: DataService = services.dataService
  val cacheService: CacheService = services.cacheService
  val lambdaService: AWSLambdaService = services.lambdaService
  val slackService: SlackEventService = services.slackEventService
  val botResultService: BotResultService = services.botResultService
  val slackFileMap: SlackFileMap = services.slackFileMap

  class InvalidTokenException extends Exception

  private def logAndRespondFor(status: Status, maybeErrorData: Option[APIErrorData], maybeFormErrors: Option[Seq[FormError]], details: JsValue = JsObject.empty)(implicit r: Request[AnyContent]): Result = {
    val formErrors = maybeFormErrors.map { errors =>
      errors.map { error =>
        APIErrorData(error.format, Some(error.key).filter(_.nonEmpty))
      }
    }.getOrElse(Seq())
    val errorMessage = maybeErrorData.map { data =>
      data.field.map { field =>
        s"$field: ${data.message}"
      }.getOrElse(data.message)
    }.getOrElse("")
    val errorResultData = APIResultWithErrorsData(formErrors ++ Seq(maybeErrorData).flatten)
    val jsonErrorResultData = Json.toJson(errorResultData)
    val result = status.apply(jsonErrorResultData)
    Logger.info(
      s"""Returning a ${result.header.status} for: $errorMessage
         |
         |${Json.prettyPrint(jsonErrorResultData)}
         |
         |Api info: ${Json.prettyPrint(details)}
         |
         |Request: $r with ${r.rawQueryString} ${r.body}""".stripMargin)
    result
  }

  private def badRequest(maybeApiErrorData: Option[APIErrorData], maybeFormErrors: Option[Seq[FormError]], details: JsValue = JsObject.empty)(implicit r: Request[AnyContent]): Result = {
    logAndRespondFor(BadRequest, maybeApiErrorData, maybeFormErrors, details)
  }

  private def notFound(apiErrorData: APIErrorData, details: JsValue = JsObject.empty)(implicit r: Request[AnyContent]): Result = {
    logAndRespondFor(NotFound, Some(apiErrorData), None, details)
  }

  private def invalidTokenRequest[T](details: T = None)(implicit r: Request[AnyContent], tjs: Writes[T]): Result = {
    badRequest(Some(APIErrorData("Invalid token", Some("token"))), None, Json.toJson(details))
  }

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

    def maybeOriginatingBehaviorVersion: Future[Option[BehaviorVersion]] = {
      maybeInvocationToken.map { invocationToken =>
        dataService.behaviorVersions.findWithoutAccessCheck(invocationToken.behaviorVersionId)
      }.getOrElse(Future.successful(None))
    }

    def maybeBehaviorVersionFor(actionName: String, maybeOriginatingBehaviorVersion: Option[BehaviorVersion]) = {
      for {
        maybeGroupVersion <- Future.successful(maybeOriginatingBehaviorVersion.map(_.groupVersion))
        maybeBehaviorVersion <- maybeGroupVersion.map { groupVersion =>
          dataService.behaviorVersions.findByName(actionName, groupVersion)
        }.getOrElse(Future.successful(None))
      } yield maybeBehaviorVersion
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
            SlackMessage.fromUnformattedText(message, botProfile),
            None,
            SlackTimestamp.now,
            maybeOriginalEventType,
            isUninterruptedConversation = false,
            isEphemeral = false,
            None
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

  private def runBehaviorFor(
                              maybeEvent: Option[Event],
                              context: ApiMethodContext,
                              eitherOriginatingBehaviorOrTriggerText: Either[BehaviorVersion, String]
                            ) = {
    for {
      result <- maybeEvent.map { event =>
        for {
          result <- eventHandler.handle(event, None).map { results =>
            results.foreach { result =>
              botResultService.sendIn(result, None).map { _ =>
                Logger.info(event.logTextFor(result, Some("in response to API run request")))
              }.recover {
                case c: SlackMessageSenderChannelException => {
                  (for {
                    botProfile <- context.maybeBotProfile
                    slackUserProfile <- context.maybeSlackProfile
                  } yield {
                    val description = descriptionForResult(result, eitherOriginatingBehaviorOrTriggerText)
                    val messageStart = if (maybeEvent.map(_.originalEventType).contains(EventType.scheduled)) {
                      s"**I was unable to complete a scheduled action in the specified channel** — $description"
                    } else {
                      s"**I was unable to complete an action in the specified channel** — $description"
                    }
                    val message =
                      s"""${messageStart}
                         |
                         |${c.formattedChannelReason}""".stripMargin
                    dataService.slackBotProfiles.sendDMWarningMessageFor(event, services, botProfile, slackUserProfile.loginInfo.providerKey, message)
                  }).getOrElse {
                    throw c
                  }
                }
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

  private def descriptionForResult(result: BotResult, eitherOriginatingBehaviorOrTriggerText: Either[BehaviorVersion, String]): String = {
    val maybeActionName = result.maybeBehaviorVersion.flatMap(_.maybeName)
    val actionBeingRun = maybeActionName.map(name =>
      s"the action named `$name`"
    ).getOrElse("an action")

    val originatingText = eitherOriginatingBehaviorOrTriggerText.fold(
      behaviorVersion => {
        behaviorVersion.maybeName.
          map(name => s", triggered by `$name`,").
          getOrElse(", triggered by another action,")
      },
      triggerText => {
        s", triggered by the message `$triggerText`"
      }
    )

    val maybeGroupName = result.maybeBehaviorVersion.map(_.groupVersion.name)
    val skillName = maybeGroupName.map(groupName =>
      s" in the skill `${groupName}`"
    ).getOrElse("")

    actionBeingRun + originatingText + skillName
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

  private def resultForFormErrors[T <: ApiMethodInfo](formWithErrors: Form[T])(implicit r: Request[AnyContent]): Result = {
    badRequest(None, Some(formWithErrors.errors))
  }

  case class RunActionArgumentInfo(name: String, value: String)

  implicit val runActionArgumentInfoFormat = Json.format[RunActionArgumentInfo]

  case class RunActionInfo(
                            actionName: Option[String],
                            trigger: Option[String],
                            arguments: Seq[RunActionArgumentInfo],
                            responseContext: String,
                            channel: String,
                            token: String,
                            originalEventType: Option[String]
                          ) extends ApiMethodWithActionAndArgumentsInfo

  implicit val runActionInfoWrites = Json.writes[RunActionInfo]

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
      "token" -> nonEmptyText,
      "originalEventType" -> optional(nonEmptyText)
    )(RunActionInfo.apply)(RunActionInfo.unapply) verifying(actionNameAndTriggerError, checkActionNameAndTrigger _)
  )

  private def runByName(
                         actionName: String,
                         info: RunActionInfo,
                         context: ApiMethodContext
                       )(implicit request: Request[AnyContent]): Future[Result] = {
    for {
      maybeSlackChannelId <- context.maybeSlackChannelIdFor(info.channel)
      maybeOriginatingBehaviorVersion <- context.maybeOriginatingBehaviorVersion
      maybeBehaviorVersion <- context.maybeBehaviorVersionFor(actionName, maybeOriginatingBehaviorVersion)
      maybeEvent <- Future.successful(
        for {
          botProfile <- context.maybeBotProfile
          slackProfile <- context.maybeSlackProfile
          behaviorVersion <- maybeBehaviorVersion
        } yield RunEvent(
          botProfile,
          slackProfile.teamId,
          behaviorVersion,
          info.argumentsMap,
          maybeSlackChannelId.getOrElse(info.channel),
          None,
          slackProfile.loginInfo.providerKey,
          SlackTimestamp.now,
          info.originalEventType.flatMap(EventType.find),
          isEphemeral = false,
          None
        )
      )
      result <- (for {
        originatingBehaviorVersion <- maybeOriginatingBehaviorVersion
        behaviorVersion <- maybeBehaviorVersion
      } yield {
        runBehaviorFor(maybeEvent, context, Left(originatingBehaviorVersion))
      }).getOrElse {
        Future.successful(notFound(APIErrorData(s"Action named `$actionName` not found", Some("actionName")), Json.toJson(info)))
      }
    } yield result
  }

  private def runByTrigger(
                         trigger: String,
                         info: RunActionInfo,
                         context: ApiMethodContext
                       )(implicit request: Request[AnyContent]): Future[Result] = {
    for {
      maybeEvent <- context.maybeMessageEventFor(trigger, info.channel, EventType.maybeFrom(info.originalEventType))
      result <- runBehaviorFor(maybeEvent, context, Right(trigger))
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
              Future.successful(badRequest(Some(APIErrorData(actionNameAndTriggerError, None)), None, Json.toJson(info)))
            }
          }
        } yield result

        eventualResult.recover {
          case e: InvalidTokenException => invalidTokenRequest(info)
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

  implicit val scheduleActionInfoWrites = Json.writes[ScheduleActionInfo]

  case class ScheduleResult(
                             scheduled: Option[ScheduleActionResult],
                             unscheduled: Option[Seq[ScheduleActionResult]]
                           )

  case class ScheduleActionResult(
                                   actionName: Option[String],
                                   trigger: Option[String],
                                   arguments: Option[Seq[RunActionArgumentInfo]],
                                   recurrence: String,
                                   firstRecurrence: Option[OffsetDateTime],
                                   secondRecurrence: Option[OffsetDateTime],
                                   useDM: Boolean,
                                   channel: String
                                 )

  implicit val scheduleActionResultFormat = Json.format[ScheduleActionResult]
  implicit val scheduleResultFormat = Json.format[ScheduleResult]

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
      maybeOriginatingBehaviorVersion <- context.maybeOriginatingBehaviorVersion
      maybeBehaviorVersion <- context.maybeBehaviorVersionFor(actionName, maybeOriginatingBehaviorVersion)
      result <- (for {
        slackProfile <- context.maybeSlackProfile
        behaviorVersion <- maybeBehaviorVersion
      } yield {
        for {
          user <- dataService.users.ensureUserFor(slackProfile.loginInfo, behaviorVersion.team.id)
          maybeScheduled <- dataService.scheduledBehaviors.maybeCreateWithRecurrenceText(
            behaviorVersion.behavior,
            info.argumentsMap,
            info.recurrenceString,
            user,
            behaviorVersion.team,
            maybeSlackChannelId,
            info.useDM
          )
        } yield {
          maybeScheduled.map { scheduled =>
            Ok(Json.toJson(ScheduleResult(
              scheduled = Some(ScheduleActionResult(
                actionName = Some(actionName),
                trigger = None,
                arguments = Some(info.arguments),
                recurrence = scheduled.recurrence.displayString,
                firstRecurrence = Some(scheduled.nextSentAt),
                secondRecurrence = scheduled.maybeFollowingSentAt,
                useDM = scheduled.isForIndividualMembers,
                channel = scheduled.maybeChannel.getOrElse("(missing)")
              )),
              unscheduled = None
            )))
          }.getOrElse {
            badRequest(Some(APIErrorData(s"Unable to schedule `$actionName` for `${info.recurrenceString}`", None)), None, Json.toJson(info))
          }
        }
      }).getOrElse {
        Future.successful(notFound(APIErrorData(s"Couldn't find the action `$actionName` to schedule", Some("actionName")), Json.toJson(info)))
      }
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
    } yield {
      maybeScheduled.map { scheduled =>
        Ok(Json.toJson(ScheduleResult(
          scheduled = Some(ScheduleActionResult(
            actionName = None,
            trigger = Some(trigger),
            arguments = None,
            recurrence = scheduled.recurrence.displayString,
            firstRecurrence = Some(scheduled.nextSentAt),
            secondRecurrence = scheduled.maybeFollowingSentAt,
            useDM = scheduled.isForIndividualMembers,
            channel = scheduled.maybeChannel.getOrElse("(missing)")
          )),
          unscheduled = None
        )))
      }.getOrElse {
        badRequest(Some(APIErrorData(s"Unable to schedule `$trigger` for `${info.recurrenceString}`", None)), None, Json.toJson(info))
      }
    }
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
              Future.successful(badRequest(Some(APIErrorData(actionNameAndTriggerError, None)), None, Json.toJson(info)))
            }
          }
        } yield result

        eventualResult.recover {
          case e: InvalidTokenException => invalidTokenRequest(info)
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

  implicit val unscheduleActionInfoWrites = Json.writes[UnscheduleActionInfo]

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
      maybeSlackChannelId <- info.channel.map { channel =>
        context.maybeSlackChannelIdFor(channel)
      }.getOrElse(Future.successful(info.channel))
      maybeOriginatingBehaviorVersion <- context.maybeOriginatingBehaviorVersion
      maybeBehaviorVersion <- context.maybeBehaviorVersionFor(actionName, maybeOriginatingBehaviorVersion)
      maybeUser <- info.userId.map { userId =>
        dataService.users.find(userId)
      }.getOrElse(Future.successful(None))
      result <- maybeBehaviorVersion.map { behaviorVersion =>
        if (info.userId.isDefined && maybeUser.isEmpty) {
          Future.successful(notFound(APIErrorData(s"Couldn't find a user with ID `${info.userId.get}`", Some("userId")), Json.toJson(info)))
        } else if (info.channel.isDefined && maybeSlackChannelId.isEmpty) {
          Future.successful(notFound(APIErrorData(s"Couldn't find channel for `${info.channel}`", Some("channel")), Json.toJson(info)))
        } else {
          dataService.scheduledBehaviors.allForBehavior(behaviorVersion.behavior, maybeUser, maybeSlackChannelId).flatMap { scheduledBehaviors =>
            for {
              unscheduledList <- Future.sequence(scheduledBehaviors.map { ea =>
                dataService.scheduledBehaviors.delete(ea)
              })
            } yield {
              Ok(Json.toJson(ScheduleResult(
                scheduled = None,
                unscheduled = Some(unscheduledList.flatten.map(scheduled =>
                  ScheduleActionResult(
                    actionName = Some(actionName),
                    trigger = None,
                    arguments = Some(scheduled.arguments.map { case (key, value) => RunActionArgumentInfo(key, value) }.toSeq),
                    recurrence = scheduled.recurrence.displayString,
                    firstRecurrence = None,
                    secondRecurrence = None,
                    useDM = scheduled.isForIndividualMembers,
                    channel = scheduled.maybeChannel.getOrElse("(unknown)")
                  )
                ))
              )))
            }
          }
        }
      }.getOrElse(Future.successful(notFound(APIErrorData(s"Couldn't find an action with name `$actionName`", Some("actionName")), Json.toJson(info))))
    } yield result
  }

  private def unscheduleByTrigger(
                                   trigger: String,
                                   info: UnscheduleActionInfo,
                                   context: ApiMethodContext
                                  )(implicit request: Request[AnyContent]): Future[Result] = {
    for {
      maybeSlackChannelId <- info.channel.map { channel =>
        context.maybeSlackChannelIdFor(channel)
      }.getOrElse(Future.successful(info.channel))
      maybeUser <- info.userId.map { userId =>
        dataService.users.find(userId)
      }.getOrElse(Future.successful(None))
      result <- context.maybeTeam.map { team =>
        dataService.scheduledMessages.allForText(trigger, team, maybeUser, maybeSlackChannelId).flatMap { scheduledMessages =>
          for {
            unscheduledList <- Future.sequence(scheduledMessages.map { ea =>
              dataService.scheduledMessages.delete(ea)
            })
          } yield {
            Ok(Json.toJson(ScheduleResult(
              scheduled = None,
              unscheduled = Some(unscheduledList.flatten.map(scheduled =>
                ScheduleActionResult(
                  actionName = None,
                  trigger = Some(trigger),
                  arguments = None,
                  recurrence = scheduled.recurrence.displayString,
                  firstRecurrence = None,
                  secondRecurrence = None,
                  useDM = scheduled.isForIndividualMembers,
                  channel = scheduled.maybeChannel.getOrElse("(unknown)")
                )
              ))
            )))
          }
        }
      }.getOrElse {
        Future.successful(notFound(APIErrorData("Couldn't find team", None), Json.toJson(info)))
      }
    } yield result
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
              Future.successful(badRequest(Some(APIErrorData(actionNameAndTriggerError, None)), None, Json.toJson(info)))
            }
          }
        } yield result

        eventualResult.recover {
          case e: InvalidTokenException => invalidTokenRequest(info)
        }
      }
    )
  }

  case class AddMessageListenerInfo(
                                     actionName: String,
                                     messageInputName: String,
                                     arguments: Seq[RunActionArgumentInfo],
                                     userId: String,
                                     channel: String,
                                     threadId: Option[String],
                                     token: String
                                   ) extends ApiMethodInfo {
    val argumentsMap: Map[String, String] = {
      arguments.map { ea =>
        (ea.name, ea.value)
      }.toMap
    }
  }

  implicit val addMessageListenerInfoWrites = Json.writes[AddMessageListenerInfo]

  private val addMessageListenerForm = Form(
    mapping(
      "actionName" -> nonEmptyText,
      "messageInputName" -> nonEmptyText,
      "arguments" -> seq(
        mapping(
          "name" -> nonEmptyText,
          "value" -> nonEmptyText
        )(RunActionArgumentInfo.apply)(RunActionArgumentInfo.unapply)
      ),
      "userId" -> nonEmptyText,
      "channel" -> nonEmptyText,
      "thread" -> optional(nonEmptyText),
      "token" -> nonEmptyText
    )(AddMessageListenerInfo.apply)(AddMessageListenerInfo.unapply)
  )

  def addMessageListener = Action.async { implicit request =>
    addMessageListenerForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(resultForFormErrors(formWithErrors))
      },
      info => {
        val eventualResult = for {
          context <- ApiMethodContext.createFor(info.token)
          maybeOriginatingBehaviorVersion <- context.maybeOriginatingBehaviorVersion
          maybeBehaviorVersion <- context.maybeBehaviorVersionFor(info.actionName, maybeOriginatingBehaviorVersion)
          maybeMessageInput <- maybeBehaviorVersion.map { behaviorVersion =>
            dataService.inputs.findByName(info.messageInputName, behaviorVersion.groupVersion)
          }.getOrElse(Future.successful(None))
          result <- (for {
            slackProfile <- context.maybeSlackProfile
            behaviorVersion <- maybeBehaviorVersion
            team <- context.maybeTeam
            messageInput <- maybeMessageInput
          } yield {
            for {
              user <- dataService.users.ensureUserFor(slackProfile.loginInfo, behaviorVersion.team.id)
              listener <- dataService.messageListeners.createFor(
                behaviorVersion.behavior,
                messageInput,
                info.argumentsMap,
                user,
                team,
                info.channel,
                info.threadId
              )
            } yield {
              Ok(listener.id)
            }
          }).getOrElse {
            Future.successful(notFound(APIErrorData(s"Couldn't add listener for action `${info.actionName}` with message input `${info.messageInputName}", Some("actionName")), Json.toJson(info)))
          }
        } yield result

        eventualResult.recover {
          case e: InvalidTokenException => invalidTokenRequest(info)
        }
      }
    )
  }

  case class PostMessageInfo(
                              message: String,
                              responseContext: String,
                              channel: String,
                              token: String,
                              originalEventType: Option[String]
                            ) extends ApiMethodWithMessageInfo

  implicit val postMessageInfoWrites = Json.writes[PostMessageInfo]

  private val postMessageForm = Form(
    mapping(
      "message" -> nonEmptyText,
      "responseContext" -> nonEmptyText,
      "channel" -> nonEmptyText,
      "token" -> nonEmptyText,
      "originalEventType" -> optional(nonEmptyText)
    )(PostMessageInfo.apply)(PostMessageInfo.unapply)
  )

  def postMessage = Action.async { implicit request =>
    postMessageForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(resultForFormErrors(formWithErrors))
      },
      info => {
        val eventualResult = for {
          context <- ApiMethodContext.createFor(info.token)
          maybeEvent <- context.maybeMessageEventFor(info.message, info.channel, EventType.maybeFrom(info.originalEventType))
          result <- runBehaviorFor(maybeEvent, context, Right(info.message))
        } yield result

        eventualResult.recover {
          case e: InvalidTokenException => invalidTokenRequest(info)
        }
      }
    )

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
            val botResult = SimpleTextResult(event, None, info.message, responseType = Normal, shouldInterrupt = false)
            botResultService.sendIn(botResult, None).map { _ =>
              Ok(Json.toJson(Seq(botResult.fullText)))
            }
          }.getOrElse {
            printApiContextError(context)
            Future.successful(InternalServerError("Request failed.\n"))
          }
        } yield result

        eventualResult.recover {
          case invalidTokenException: InvalidTokenException => invalidTokenRequest(info)
          case channelException: SlackMessageSenderChannelException => {
            badRequest(Some(APIErrorData(s"""Error: ${channelException.rawChannelReason}""", Some("channel"))), None, Json.toJson(info))
          }
          case slackException: SlackMessageSenderException => {
            slackException.underlying match {
              // TODO: 400 seems like maybe the wrong kind of error here
              case apiError: SlackApiError => {
                badRequest(Some(APIErrorData(s"Slack API error: ${apiError.code}\n", None)), None, Json.toJson(info))
              }
              case _ => {
                badRequest(Some(APIErrorData(s"Unknown error while attempting to send message to Slack", None)), None, Json.toJson(info))
              }
            }

          }
        }
      }
    )

  }

  case class GenerateApiTokenInfo(
                                   token: String,
                                   expirySeconds: Option[Int],
                                   isOneTime: Option[Boolean]
                                  ) extends ApiMethodInfo

  implicit val generateApiTokenInfoWrites = Json.writes[GenerateApiTokenInfo]

  private val generateApiTokenForm = Form(
    mapping(
      "token" -> nonEmptyText,
      "expirySeconds" -> optional(number),
      "isOneTime" -> optional(boolean)
    )(GenerateApiTokenInfo.apply)(GenerateApiTokenInfo.unapply)
  )

  def generateApiToken = Action.async { implicit request =>
    generateApiTokenForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(resultForFormErrors(formWithErrors))
      },
      info => {
        val eventualResult = for {
          context <- ApiMethodContext.createFor(info.token)
          maybeNewToken <- context.maybeInvocationToken.map { invocationToken =>
            dataService.apiTokens.createFor(invocationToken, info.expirySeconds, info.isOneTime.getOrElse(false)).map(Some(_))
          }.getOrElse(Future.successful(None))
        } yield {
          maybeNewToken.map { newToken =>
            Ok(Json.toJson(APITokenData.from(newToken)))
          }.getOrElse {
            Forbidden("Invocation token has expired")
          }
        }

        eventualResult.recover {
          // TODO: look into this and similar cases and maybe do something different
          case e: InvalidTokenException => invalidTokenRequest(info)
        }
      }
    )

  }

  private def contentDispositionForContentType(contentType: String): String = {
    val extension = """image/(.*)""".r.findFirstMatchIn(contentType).flatMap { r =>
      r.subgroups.headOption
    }.getOrElse("txt")
    s"""attachment; filename="ellipsis.${extension}""""
  }

  private def contentDispositionFor(response: WSResponse, contentType: String, httpHeaders: (String, String), maybeOriginalUrl: Option[String]): Future[String] = {
    val maybeDispositionFromResponse = response.headers.get(CONTENT_DISPOSITION).flatMap(_.headOption)
    maybeDispositionFromResponse.map(Future.successful).getOrElse {
      maybeOriginalUrl.map { originalUrl =>
        ws.url(originalUrl).withHttpHeaders(httpHeaders).head.map { r =>
          r.headers.get(CONTENT_DISPOSITION).flatMap(_.headOption).getOrElse {
            contentDispositionForContentType(contentType)
          }
        }
      }.getOrElse {
        Future.successful(contentDispositionForContentType(contentType))
      }
    }
  }

  def fetchFile(token: String, fileId: String) = Action.async { implicit request =>
    val eventualResult = for {
      context <- ApiMethodContext.createFor(token)
      result <- (for {
        botProfile <- context.maybeBotProfile
        originalUrl <- slackFileMap.maybeUrlFor(fileId)
      } yield {
        val maybeThumbnailUrl = slackFileMap.maybeThumbnailUrlFor(fileId)
        val urlToUse = maybeThumbnailUrl.getOrElse(originalUrl)
        val httpHeaders = (AUTHORIZATION, s"Bearer ${botProfile.token}")
        ws.url(urlToUse).withHttpHeaders(httpHeaders).get.flatMap { r =>
          if (r.status == 200) {
            val contentType =
              r.headers.get(CONTENT_TYPE).
                flatMap(_.headOption).
                getOrElse("application/octet-stream")

            contentDispositionFor(r, contentType, httpHeaders, maybeThumbnailUrl.map(_ => originalUrl)).map { contentDisposition =>
              val result = r.headers.get(CONTENT_LENGTH) match {
                case Some(Seq(length)) =>
                  Ok.sendEntity(HttpEntity.Streamed(r.bodyAsSource, Some(length.toLong), Some(contentType)))
                case _ =>
                  Ok.chunked(r.bodyAsSource).as(contentType)
              }
              result.withHeaders(CONTENT_TYPE -> contentType, CONTENT_DISPOSITION -> contentDisposition)
            }

          } else {
            Future.successful(BadGateway)
          }
        }
      }).getOrElse(Future.successful(NotFound(s"Unable to find a file with ID $fileId")))
    } yield result

    eventualResult.recover {
      case e: InvalidTokenException => invalidTokenRequest(Map("token" -> token, "fileId" -> fileId))
    }
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

  case class FindUsersResult(users: Seq[UserData])

  implicit val findUsersResultWrites = Json.writes[FindUsersResult]

  def findUsers(token: String, maybeEmail: Option[String]) = Action.async { implicit request =>
    val eventualResult = for {
      context <- ApiMethodContext.createFor(token)
      result <- context.maybeTeam.map { team =>
        maybeEmail.map { email =>
          dataService.users.maybeUserDataForEmail(email, team).map { maybeUserData =>
            val users = Seq(maybeUserData).flatten
            Logger.info(s"Sending ${users.length} user data object(s) on team ${team.id} for findUsers API request")
            Ok(Json.toJson(FindUsersResult(users)))
          }
        }.getOrElse {
          Logger.warn(s"findUsers API request with no email param")
          Future.successful(badRequest(Some(APIErrorData("You must pass an `email` parameter to find.", None)), None))
        }
      }.getOrElse {
        Logger.warn(s"findUsers API request with no valid team")
        Future.successful(notFound(APIErrorData("Team not found", None)))
      }
    } yield result

    eventualResult.recover {
      case e: InvalidTokenException => invalidTokenRequest(Map("token" -> token))
    }
  }
}
