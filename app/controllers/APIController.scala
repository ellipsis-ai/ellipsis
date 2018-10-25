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
import models.behaviors.testing.{TestMessageEvent, TestRunEvent}
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

  trait ApiMethodContext {

    val maybeInvocationToken: Option[InvocationToken]
    val maybeUser: Option[User]
    val maybeScheduledMessage: Option[ScheduledMessage]
    val maybeTeam: Option[Team]
    val isInvokedExternally: Boolean

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

    def maybeBaseMessageEventFor(message: String, channel: String, maybeOriginalEventType: Option[EventType]): Future[Option[Event]]

    def maybeMessageEventFor(message: String, channel: String, maybeOriginalEventType: Option[EventType]): Future[Option[Event]] = {
      maybeBaseMessageEventFor(message, channel, maybeOriginalEventType).map { maybeBaseEvent =>
        maybeBaseEvent.map { messageEvent =>
          val event: Event = maybeScheduledMessage.map { scheduledMessage =>
            ScheduledEvent(messageEvent, scheduledMessage)
          }.getOrElse(messageEvent)
          event
        }
      }
    }

    def runEventFor(
                      behaviorVersion: BehaviorVersion,
                      argumentsMap: Map[String, String],
                      channel: String,
                      maybeOriginalEventType: Option[EventType]
                    ): Future[Event]

    def maybeRunEventForName(
                              actionName: String,
                              argumentsMap: Map[String, String],
                              channel: String,
                              maybeOriginalEventType: Option[EventType],
                              maybeOriginatingBehaviorVersion: Option[BehaviorVersion]
                            ): Future[Option[Event]] = {
      for {
        maybeBehaviorVersion <- maybeBehaviorVersionFor(actionName, maybeOriginatingBehaviorVersion)
        maybeEvent <- maybeBehaviorVersion.map { behaviorVersion =>
          runEventFor(behaviorVersion, argumentsMap, channel, maybeOriginalEventType).map(Some(_))
        }.getOrElse(Future.successful(None))
      } yield maybeEvent
    }

    protected def sendFor(event: Event, result: BotResult, eitherOriginatingBehaviorOrTriggerText: Either[BehaviorVersion, String]): Future[Unit] = {
      botResultService.sendIn(result, None).map { _ =>
        Logger.info(event.logTextFor(result, Some("in response to API run request")))
      }
    }

    def runBehaviorFor(
                        maybeEvent: Option[Event],
                        eitherOriginatingBehaviorOrTriggerText: Either[BehaviorVersion, String]
                      ) = {
      for {
        result <- maybeEvent.map { event =>
          for {
            result <- eventHandler.handle(event, None).map { results =>
              results.foreach { result =>
                sendFor(event, result, eitherOriginatingBehaviorOrTriggerText)
              }
              Ok(Json.toJson(results.map(_.fullText)))
            }
          } yield result
        }.getOrElse {
          printEventCreationError()
          Future.successful(InternalServerError("Request failed.\n"))
        }
      } yield result
    }

    def fetchFileResultFor(fileId: String)(implicit r: Request[AnyContent]): Future[Result] = {
      Future.successful(BadRequest(""))
    }

    def scheduleByName(
                        actionName: String,
                        info: ScheduleActionInfo
                      )(implicit request: Request[AnyContent]): Future[Result] = {
      Future.successful(BadRequest(""))
    }

    def scheduleByTrigger(
                           trigger: String,
                           info: ScheduleActionInfo
                         )(implicit request: Request[AnyContent]): Future[Result] = {
      Future.successful(BadRequest(""))
    }

    def unscheduleByName(
                          actionName: String,
                          info: UnscheduleActionInfo
                        )(implicit request: Request[AnyContent]): Future[Result] = {
      Future.successful(BadRequest(""))
    }

    def unscheduleByTrigger(
                            trigger: String,
                            info: UnscheduleActionInfo
                          )(implicit request: Request[AnyContent]): Future[Result] = {
      Future.successful(BadRequest(""))
    }

    def addMessageListener(info: AddMessageListenerInfo)(implicit request: Request[AnyContent]): Future[Result] = {
      Future.successful(BadRequest(""))
    }

    def printEventCreationError(): Unit

  }

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

  case class SlackApiMethodContext(
                                    maybeInvocationToken: Option[InvocationToken],
                                    maybeUser: Option[User],
                                    botProfile: SlackBotProfile,
                                    slackProfile: SlackProfile,
                                    maybeScheduledMessage: Option[ScheduledMessage],
                                    maybeTeam: Option[Team],
                                    isInvokedExternally: Boolean
                                  ) extends ApiMethodContext {

    val maybeBotProfile: Option[SlackBotProfile] = Some(botProfile)
    val maybeSlackProfile: Option[SlackProfile] = Some(slackProfile)

    def maybeSlackChannelIdFor(channel: String): Future[Option[String]] = {
      dataService.slackBotProfiles.channelsFor(botProfile).maybeIdFor(channel)
    }

    def messageEventFor(message: String, channel: String, maybeOriginalEventType: Option[EventType]): Future[Event] = {
      maybeSlackChannelIdFor(channel).map { maybeSlackChannelId =>
        SlackMessageEvent(
          botProfile,
          maybeSlackChannelId.getOrElse(channel),
          None,
          slackProfile.loginInfo.providerKey,
          SlackMessage.fromUnformattedText(message, botProfile),
          None,
          SlackTimestamp.now,
          maybeOriginalEventType,
          isUninterruptedConversation = false,
          isEphemeral = false,
          None,
          beQuiet = false
        )
      }
    }

    def maybeBaseMessageEventFor(message: String, channel: String, maybeOriginalEventType: Option[EventType]): Future[Option[Event]] = {
      messageEventFor(message, channel, maybeOriginalEventType).map(Some(_))
    }

    def runEventFor(
                     behaviorVersion: BehaviorVersion,
                     argumentsMap: Map[String, String],
                     channel: String,
                     maybeOriginalEventType: Option[EventType]
                   ): Future[Event] = {
      maybeSlackChannelIdFor(channel).map { maybeSlackChannelId =>
        RunEvent(
          botProfile,
          behaviorVersion,
          argumentsMap,
          maybeSlackChannelId.getOrElse(channel),
          None,
          slackProfile.loginInfo.providerKey,
          SlackTimestamp.now,
          maybeOriginalEventType,
          isEphemeral = false,
          None
        )
      }
    }

    override def sendFor(event: Event, result: BotResult, eitherOriginatingBehaviorOrTriggerText: Either[BehaviorVersion, String]): Future[Unit] = {
      super.sendFor(event, result, eitherOriginatingBehaviorOrTriggerText).recover {
        case c: SlackMessageSenderChannelException => {
          val description = descriptionForResult(result, eitherOriginatingBehaviorOrTriggerText)
          val messageStart = if (event.originalEventType == EventType.scheduled) {
            s"**I was unable to complete a scheduled action in the specified channel** — $description"
          } else {
            s"**I was unable to complete an action in the specified channel** — $description"
          }
          val message =
            s"""${messageStart}
               |
               |${c.formattedChannelReason}""".stripMargin
          dataService.slackBotProfiles.sendDMWarningMessageFor(event, services, botProfile, slackProfile.loginInfo.providerKey, message)
        }
      }
    }

    override def fetchFileResultFor(fileId: String)(implicit r: Request[AnyContent]): Future[Result] = {
      slackFileMap.maybeUrlFor(fileId).map { originalUrl =>
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
      }.getOrElse(Future.successful(NotFound(s"Unable to find a file with ID $fileId")))
    }

    override def scheduleByName(
                        actionName: String,
                        info: ScheduleActionInfo
                      )(implicit request: Request[AnyContent]): Future[Result] = {
      for {
        maybeSlackChannelId <- maybeSlackChannelIdFor(info.channel)
        maybeOriginatingBehaviorVersion <- maybeOriginatingBehaviorVersion
        maybeBehaviorVersion <- maybeBehaviorVersionFor(actionName, maybeOriginatingBehaviorVersion)
        result <- (for {
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

    override def scheduleByTrigger(
                           trigger: String,
                           info: ScheduleActionInfo
                         )(implicit request: Request[AnyContent]): Future[Result] = {
      for {
        maybeSlackChannelId <- maybeSlackChannelIdFor(info.channel)
        maybeScheduled <- (for {
          team <- maybeTeam
          user <- maybeUser
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

    override def unscheduleByName(
                                   actionName: String,
                                   info: UnscheduleActionInfo
                                 )(implicit request: Request[AnyContent]): Future[Result] = {
      for {
        maybeSlackChannelId <- info.channel.map { channel =>
          maybeSlackChannelIdFor(channel)
        }.getOrElse(Future.successful(info.channel))
        maybeOriginatingBehaviorVersion <- maybeOriginatingBehaviorVersion
        maybeBehaviorVersion <- maybeBehaviorVersionFor(actionName, maybeOriginatingBehaviorVersion)
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

    override def unscheduleByTrigger(
                             trigger: String,
                             info: UnscheduleActionInfo
                           )(implicit request: Request[AnyContent]): Future[Result] = {
      for {
        maybeSlackChannelId <- info.channel.map { channel =>
          maybeSlackChannelIdFor(channel)
        }.getOrElse(Future.successful(info.channel))
        maybeUser <- info.userId.map { userId =>
          dataService.users.find(userId)
        }.getOrElse(Future.successful(None))
        result <- maybeTeam.map { team =>
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

    override def addMessageListener(info: AddMessageListenerInfo)(implicit request: Request[AnyContent]): Future[Result] = {
      for {
        maybeOriginatingBehaviorVersion <- maybeOriginatingBehaviorVersion
        maybeBehaviorVersion <- maybeBehaviorVersionFor(info.actionName, maybeOriginatingBehaviorVersion)
        result <- (for {
          behaviorVersion <- maybeBehaviorVersion
          team <- maybeTeam
        } yield {
          for {
            user <- dataService.users.ensureUserFor(slackProfile.loginInfo, behaviorVersion.team.id)
            listener <- dataService.messageListeners.createFor(
              behaviorVersion.behavior,
              info.argumentsMap,
              user,
              team,
              info.medium,
              info.channel,
              info.threadId
            )
          } yield {
            Ok(listener.id)
          }
        }).getOrElse {
          Future.successful(notFound(APIErrorData(s"Couldn't add listener for action `${info.actionName}`", Some("actionName")), Json.toJson(info)))
        }
      } yield result
    }

    def printEventCreationError(): Unit = {
      Logger.error(
        s"""Event creation likely failed for API context:
           |
           |Slack bot profile ID: ${botProfile.userId}
           |Slack user profile ID: ${slackProfile.loginInfo.providerID}
           |""".stripMargin
      )
    }

  }

  object SlackApiMethodContext {

    def maybeCreateFor(token: String): Future[Option[SlackApiMethodContext]] = {
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
        maybeSlackProfile <- maybeUser.map { user =>
          dataService.users.maybeSlackProfileFor(user)
        }.getOrElse(Future.successful(None))
        maybeSlackTeamIdForBot <- Future.successful {
          if (maybeUserForApiToken.isDefined) {
            // TODO: This makes it impossible for a user's API token to work on other Slack Enterprise Grid workspaces
            maybeSlackProfile.map(_.teamIds.primary)
          } else {
            maybeInvocationToken.flatMap(_.maybeTeamIdForContext)
          }
        }
        maybeBotProfile <- maybeSlackTeamIdForBot.map { slackTeamId =>
          dataService.slackBotProfiles.allForSlackTeamId(slackTeamId).map(_.headOption)
        }.getOrElse(Future.successful(None))
      } yield {
        for {
          botProfile <- maybeBotProfile
          slackProfile <- maybeSlackProfile
        } yield {
          SlackApiMethodContext(
            maybeInvocationToken,
            maybeUser,
            botProfile,
            slackProfile,
            maybeScheduledMessage,
            maybeTeam,
            isInvokedExternally = maybeUserForApiToken.isDefined
          )
        }
      }
    }
  }

  case class NoMediumApiMethodContext(
                                       maybeInvocationToken: Option[InvocationToken],
                                       user: User,
                                       team: Team,
                                       maybeScheduledMessage: Option[ScheduledMessage],
                                       isInvokedExternally: Boolean
                                     ) extends ApiMethodContext {
    val maybeUser: Option[User] = Some(user)
    val maybeTeam: Option[Team] = Some(team)
    val maybeBotProfile: Option[SlackBotProfile] = None
    val maybeSlackProfile: Option[SlackProfile] = None

    def maybeBaseMessageEventFor(message: String, channel: String, maybeOriginalEventType: Option[EventType]): Future[Option[Event]] = {
      Future.successful(Some(TestMessageEvent(user, team, message, includesBotMention = true)))
    }

    def runEventFor(
                     behaviorVersion: BehaviorVersion,
                     argumentsMap: Map[String, String],
                     channel: String,
                     maybeOriginalEventType: Option[EventType]
                   ): Future[Event] = {
      Future.successful(
        TestRunEvent(
          user,
          team,
          behaviorVersion,
          argumentsMap
        )
      )
    }

    def printEventCreationError(): Unit = {
      Logger.error(
        s"""Event creation likely failed for API no-medium context:
           |
           |User ID: ${user.id}
           |Team ID: ${team.id}
           |""".stripMargin
      )
    }
  }

  object NoMediumApiMethodContext {

    def maybeCreateFor(token: String): Future[Option[NoMediumApiMethodContext]] = {
      for {
        maybeUserForApiToken <- maybeUserForApiToken(token)
        maybeInvocationToken <- dataService.invocationTokens.findNotExpired(token)
        maybeUserForInvocationToken <- dataService.users.findForInvocationToken(token)
        maybeUser <- Future.successful(maybeUserForApiToken.orElse(maybeUserForInvocationToken))
        maybeTeam <- maybeUser.map { user =>
          dataService.teams.find(user.teamId)
        }.getOrElse {
          throw new InvalidTokenException()
        }
        maybeScheduledMessage <- maybeInvocationToken.flatMap { token =>
          token.maybeScheduledMessageId.map { msgId =>
            dataService.scheduledMessages.find(msgId)
          }
        }.getOrElse(Future.successful(None))
      } yield {
        for {
          user <- maybeUser
          team <- maybeTeam
        } yield {
          NoMediumApiMethodContext(
            maybeInvocationToken,
            user,
            team,
            maybeScheduledMessage,
            isInvokedExternally = maybeUserForApiToken.isDefined
          )
        }
      }
    }
  }

  class APIMethodContextBuilderException extends Exception

  object ApiMethodContextBuilder {

    def createFor(token: String): Future[ApiMethodContext] = {
      SlackApiMethodContext.maybeCreateFor(token).flatMap { maybeSlackMethodContext: Option[ApiMethodContext] =>
        maybeSlackMethodContext.map(Future.successful).getOrElse {
          NoMediumApiMethodContext.maybeCreateFor(token).map { maybeNoMediumMethodContext =>
            maybeNoMediumMethodContext.getOrElse {
              throw new APIMethodContextBuilderException
            }
          }
        }
      }
    }

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
      maybeOriginatingBehaviorVersion <- context.maybeOriginatingBehaviorVersion
      maybeBehaviorVersion <- context.maybeBehaviorVersionFor(actionName, maybeOriginatingBehaviorVersion)
      maybeEvent <- context.maybeRunEventForName(
        actionName,
        info.argumentsMap,
        info.channel,
        info.originalEventType.flatMap(EventType.find),
        None
      )
      result <- (for {
        originatingBehaviorVersion <- maybeOriginatingBehaviorVersion
        behaviorVersion <- maybeBehaviorVersion
      } yield {
        context.runBehaviorFor(maybeEvent, Left(originatingBehaviorVersion))
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
      result <- context.runBehaviorFor(maybeEvent, Right(trigger))
    } yield result
  }

  def runAction = Action.async { implicit request =>
    runActionForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(resultForFormErrors(formWithErrors))
      },
      info => {
        val eventualResult = for {
          context <- ApiMethodContextBuilder.createFor(info.token)
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

  def scheduleAction = Action.async { implicit request =>
    scheduleActionForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(resultForFormErrors(formWithErrors))
      },
      info => {
        val eventualResult = for {
          context <- ApiMethodContextBuilder.createFor(info.token)
          result <- info.actionName.map { actionName =>
            context.scheduleByName(actionName, info)
          }.getOrElse {
            info.trigger.map { trigger =>
              context.scheduleByTrigger(trigger, info)
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

  def unscheduleAction = Action.async { implicit request =>
    unscheduleActionForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(resultForFormErrors(formWithErrors))
      },
      info => {
        val eventualResult = for {
          context <- ApiMethodContextBuilder.createFor(info.token)
          result <- info.actionName.map { actionName =>
            context.unscheduleByName(actionName, info)
          }.getOrElse {
            info.trigger.map { trigger =>
              context.unscheduleByTrigger(trigger, info)
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
                                     arguments: Seq[RunActionArgumentInfo],
                                     userId: String,
                                     medium: String,
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
      "arguments" -> seq(
        mapping(
          "name" -> nonEmptyText,
          "value" -> nonEmptyText
        )(RunActionArgumentInfo.apply)(RunActionArgumentInfo.unapply)
      ),
      "userId" -> nonEmptyText,
      "medium" -> nonEmptyText,
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
          context <- ApiMethodContextBuilder.createFor(info.token)
          result <- context.addMessageListener(info)
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
          context <- ApiMethodContextBuilder.createFor(info.token)
          maybeEvent <- context.maybeMessageEventFor(info.message, info.channel, EventType.maybeFrom(info.originalEventType))
          result <- context.runBehaviorFor(maybeEvent, Right(info.message))
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
          context <- ApiMethodContextBuilder.createFor(info.token)
          maybeEvent <- context.maybeMessageEventFor(info.message, info.channel, EventType.maybeFrom(info.originalEventType))
          result <- maybeEvent.map { event =>
            val botResult = SimpleTextResult(event, None, info.message, responseType = Normal, shouldInterrupt = false)
            botResultService.sendIn(botResult, None).map { _ =>
              Ok(Json.toJson(Seq(botResult.fullText)))
            }
          }.getOrElse {
            context.printEventCreationError()
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
          context <- ApiMethodContextBuilder.createFor(info.token)
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
      context <- ApiMethodContextBuilder.createFor(token)
      result <- context.fetchFileResultFor(fileId)
    } yield result

    eventualResult.recover {
      case e: InvalidTokenException => invalidTokenRequest(Map("token" -> token, "fileId" -> fileId))
    }
  }

  case class FindUsersResult(users: Seq[UserData])

  implicit val findUsersResultWrites = Json.writes[FindUsersResult]

  def findUsers(token: String, maybeEmail: Option[String]) = Action.async { implicit request =>
    val eventualResult = for {
      context <- ApiMethodContextBuilder.createFor(token)
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

  case class DeleteSavedAnswersResult(inputName: String, deletedCount: Int)

  implicit val deleteSavedAnswersResultWrites = Json.writes[DeleteSavedAnswersResult]

  case class DeleteSavedAnswersInfo(inputName: String, deleteAll: Option[Boolean], token: String) extends ApiMethodInfo

  implicit val deleteSavedAnswersInfoWrites = Json.writes[DeleteSavedAnswersInfo]

  private val deleteSavedAnswersForm = Form(
    mapping(
      "inputName" -> nonEmptyText,
      "deleteAll" -> optional(boolean),
      "token" -> nonEmptyText
    )(DeleteSavedAnswersInfo.apply)(DeleteSavedAnswersInfo.unapply)
  )

  private def deleteSavedAnswersFor(deleteSavedAnswersInfo: DeleteSavedAnswersInfo)
                                   (implicit r: Request[AnyContent]): Future[Result] = {
    val token = deleteSavedAnswersInfo.token
    val inputName = deleteSavedAnswersInfo.inputName
    val deleteAll = deleteSavedAnswersInfo.deleteAll.getOrElse(false)
    val eventualResult = for {
      context <- ApiMethodContextBuilder.createFor(token)
      maybeBehaviorVersion <- context.maybeOriginatingBehaviorVersion
      savedInputs <- maybeBehaviorVersion.map { behaviorVersion =>
        dataService.inputs.allForGroupVersion(behaviorVersion.groupVersion).map { inputs =>
          inputs.filter(input => input.isSaved && input.name == inputName)
        }
      }.getOrElse(Future.successful(Seq()))
      numDeleted <- {
        for {
          user <- context.maybeUser
        } yield {
          Future.sequence {
            if (deleteAll) {
              savedInputs.map(input => dataService.savedAnswers.deleteAllFor(input.inputId))
            } else {
              savedInputs.map(input => dataService.savedAnswers.deleteForUser(input.inputId, user))
            }
          }
        }
      }.getOrElse(Future.successful(Seq(0))).map(_.sum)
    } yield {
      if (savedInputs.nonEmpty) {
        Ok(Json.toJson(DeleteSavedAnswersResult(inputName, numDeleted)))
      } else {
        NotFound(s"No saved input named `${inputName}` found")
      }
    }
    eventualResult.recover {
      case e: InvalidTokenException => invalidTokenRequest(deleteSavedAnswersInfo)
    }
  }

  def deleteUserSavedAnswer(inputName: String, token: String) = Action.async { implicit request =>
    deleteSavedAnswersFor(DeleteSavedAnswersInfo(inputName, Some(false), token))
  }

  def deleteTeamSavedAnswers(inputName: String, token: String) = Action.async { implicit request =>
    deleteSavedAnswersFor(DeleteSavedAnswersInfo(inputName, Some(true), token))
  }

  def deleteSavedAnswers = Action.async { implicit request =>
    deleteSavedAnswersForm.bindFromRequest.fold(
      formWithErrors => Future.successful(resultForFormErrors(formWithErrors)),
      info => deleteSavedAnswersFor(info)
    )
  }
}
