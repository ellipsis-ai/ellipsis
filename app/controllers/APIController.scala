package controllers

import java.time.OffsetDateTime
import javax.inject.Inject

import akka.actor.ActorSystem
import com.google.inject.Provider
import json.{APIErrorData, APIResultWithErrorsData, APITokenData}
import json.Formatting._
import models.accounts.slack.botprofile.SlackBotProfile
import models.accounts.slack.profile.SlackProfile
import models.accounts.user.User
import models.behaviors.events._
import models.behaviors.invocationtoken.InvocationToken
import models.behaviors.scheduling.scheduledmessage.ScheduledMessage
import models.behaviors.{BotResultService, SimpleTextResult}
import models.team.Team
import play.api.data.Form
import play.api.data.FormError
import play.api.data.Forms._
import play.api.http.HttpEntity
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.mvc.{AnyContent, Request, Result}
import play.api.{Configuration, Logger}
import services.{AWSLambdaService, CacheService, DataService, SlackEventService}
import utils.{SlackFileMap, SlackTimestamp}

import scala.concurrent.{ExecutionContext, Future}

class APIController @Inject() (
                                val configuration: Configuration,
                                val dataService: DataService,
                                val cacheService: CacheService,
                                val lambdaService: AWSLambdaService,
                                val ws: WSClient,
                                val slackService: SlackEventService,
                                val eventHandler: EventHandler,
                                val botResultService: BotResultService,
                                val assetsProvider: Provider[RemoteAssets],
                                val slackFileMap: SlackFileMap,
                                implicit val actorSystem: ActorSystem,
                                implicit val ec: ExecutionContext
                              )
  extends EllipsisController {

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
          slackService.clientFor(botProfile),
          info.originalEventType.flatMap(EventType.find)
        )
      )
      result <- if (maybeBehavior.isDefined) {
        runBehaviorFor(maybeEvent, context)
      } else {
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
        } yield {
          maybeScheduled.map { scheduled =>
            Ok(Json.toJson(ScheduleResult(
              scheduled = Some(ScheduleActionResult(
                actionName = Some(actionName),
                trigger = None,
                arguments = Some(info.arguments),
                recurrence = scheduled.recurrence.displayString,
                firstRecurrence = Some(scheduled.nextSentAt),
                secondRecurrence = Some(scheduled.followingSentAt),
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
            secondRecurrence = Some(scheduled.followingSentAt),
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
      maybeBehavior <- context.maybeBehaviorFor(actionName)
      maybeUser <- info.userId.map { userId =>
        dataService.users.find(userId)
      }.getOrElse(Future.successful(None))
      result <- maybeBehavior.map { behavior =>
        if (info.userId.isDefined && maybeUser.isEmpty) {
          Future.successful(notFound(APIErrorData(s"Couldn't find a user with ID `${info.userId.get}`", Some("userId")), Json.toJson(info)))
        } else {
          dataService.scheduledBehaviors.allForBehavior(behavior, maybeUser, maybeSlackChannelId).flatMap { scheduledBehaviors =>
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
          result <- runBehaviorFor(maybeEvent, context)
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
          case e: InvalidTokenException => invalidTokenRequest(info)
          case e: slack.api.ApiError => if (e.code == "channel_not_found") {
            badRequest(Some(APIErrorData(s"""Error: the channel "${info.channel}" could not be found.""", Some("channel"))), None, Json.toJson(info))
          } else {
            // TODO: 400 seems like maybe the wrong kind of error here
            badRequest(Some(APIErrorData(s"Slack API error: ${e.code}\n", None)), None, Json.toJson(info))
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
}
