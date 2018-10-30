package controllers.api.context

import akka.actor.ActorSystem
import controllers.api.APIResponder
import controllers.api.exceptions.InvalidTokenException
import controllers.api.json._
import controllers.api.json.Formatting._
import json.APIErrorData
import models.accounts.slack.botprofile.SlackBotProfile
import models.accounts.slack.profile.SlackProfile
import models.accounts.user.User
import models.behaviors.BotResult
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.events._
import models.behaviors.invocationtoken.InvocationToken
import models.behaviors.scheduling.scheduledmessage.ScheduledMessage
import models.team.Team
import play.api.Logger
import play.api.http.HttpEntity
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import play.api.mvc.{AnyContent, Request, Result}
import services.DefaultServices
import utils.{SlackMessageSenderChannelException, SlackTimestamp}

import scala.concurrent.{ExecutionContext, Future}

case class SlackApiMethodContext(
                                  maybeInvocationToken: Option[InvocationToken],
                                  maybeUser: Option[User],
                                  botProfile: SlackBotProfile,
                                  slackProfile: SlackProfile,
                                  maybeScheduledMessage: Option[ScheduledMessage],
                                  maybeTeam: Option[Team],
                                  isInvokedExternally: Boolean,
                                  services: DefaultServices,
                                  responder: APIResponder,
                                  implicit val ec: ExecutionContext,
                                  implicit val actorSystem: ActorSystem
                                ) extends ApiMethodContext {

  val slackFileMap = services.slackFileMap
  val ws = services.ws

  val mediumText: String = "Slack"

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
            responder.badRequest(Some(APIErrorData(s"Unable to schedule `$actionName` for `${info.recurrenceString}`", None)), None, Json.toJson(info))
          }
        }
      }).getOrElse {
        Future.successful(responder.notFound(APIErrorData(s"Couldn't find the action `$actionName` to schedule", Some("actionName")), Json.toJson(info)))
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
        responder.badRequest(Some(APIErrorData(s"Unable to schedule `$trigger` for `${info.recurrenceString}`", None)), None, Json.toJson(info))
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
          Future.successful(responder.notFound(APIErrorData(s"Couldn't find a user with ID `${info.userId.get}`", Some("userId")), Json.toJson(info)))
        } else if (info.channel.isDefined && maybeSlackChannelId.isEmpty) {
          Future.successful(responder.notFound(APIErrorData(s"Couldn't find channel for `${info.channel}`", Some("channel")), Json.toJson(info)))
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
      }.getOrElse(Future.successful(responder.notFound(APIErrorData(s"Couldn't find an action with name `$actionName`", Some("actionName")), Json.toJson(info))))
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
        Future.successful(responder.notFound(APIErrorData("Couldn't find team", None), Json.toJson(info)))
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
        Future.successful(responder.notFound(APIErrorData(s"Couldn't add listener for action `${info.actionName}`", Some("actionName")), Json.toJson(info)))
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

  def maybeCreateFor(
                      token: String,
                      services: DefaultServices,
                      responder: APIResponder
                    )(implicit ec: ExecutionContext, actorSystem: ActorSystem): Future[Option[SlackApiMethodContext]] = {
    val dataService = services.dataService
    for {
      maybeUserForApiToken <- dataService.apiTokens.maybeUserForApiToken(token)
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
          isInvokedExternally = maybeUserForApiToken.isDefined,
          services,
          responder,
          ec,
          actorSystem
        )
      }
    }
  }
}
