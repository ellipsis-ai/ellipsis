package controllers.api.context

import java.io.File

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
import models.behaviors.events.slack.{SlackMessage, SlackMessageEvent, SlackRunEvent}
import models.behaviors.invocationtoken.InvocationToken
import models.behaviors.scheduling.scheduledmessage.ScheduledMessage
import models.team.Team
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, Request, Result}
import services.DefaultServices
import utils.SlackMessageSenderChannelException

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

  val slackApiService = services.slackApiService

  val mediumText: String = "Slack"

  val requiresChannel: Boolean = true

  def maybeSlackChannelIdFor(maybeChannel: Option[String]): Future[Option[String]] = {
    maybeChannel.map { channel =>
      dataService.slackBotProfiles.channelsFor(botProfile).maybeIdFor(channel)
    }.getOrElse(Future.successful(None))
  }

  def maybeMessageEventFor(
                            message: String,
                            maybeChannel: Option[String],
                            maybeOriginalEventType: Option[EventType],
                            maybeMessageTs: Option[String],
                            maybeThreadId: Option[String]
                          ): Future[Option[Event]] = {
    maybeSlackChannelIdFor(maybeChannel).map { maybeSlackChannelId =>
      val maybeChannelToUse = maybeSlackChannelId.orElse(maybeChannel)
      maybeChannelToUse.map { channel =>
        SlackMessageEvent(
          SlackEventContext(
            botProfile,
            maybeSlackChannelId.getOrElse(channel),
            maybeThreadId,
            slackProfile.loginInfo.providerKey
          ),
          SlackMessage.fromUnformattedText(message, botProfile, maybeMessageTs, maybeThreadId),
          maybeFile = None,
          maybeTs = maybeMessageTs,
          maybeOriginalEventType = maybeOriginalEventType,
          maybeScheduled = maybeScheduledMessage,
          isUninterruptedConversation = false,
          isEphemeral = false,
          maybeResponseUrl = None,
          beQuiet = false
        )
      }
    }
  }

  def maybeRunEventFor(
                   behaviorVersion: BehaviorVersion,
                   argumentsMap: Map[String, String],
                   maybeChannel: Option[String],
                   eventType: EventType,
                   maybeOriginalEventType: Option[EventType],
                   maybeTriggeringMessageId: Option[String],
                   maybeTriggeringMessageThreadId: Option[String]
                 ): Future[Option[SlackRunEvent]] = {
    for {
      maybeChannelToUse <- maybeSlackChannelIdFor(maybeChannel).map { maybeSlackChannelId =>
        maybeSlackChannelId.orElse(maybeChannel)
      }
    } yield {
      maybeChannelToUse.map { channel =>
        SlackRunEvent(
          SlackEventContext(
            botProfile,
            channel,
            maybeTriggeringMessageThreadId,
            slackProfile.loginInfo.providerKey
          ),
          behaviorVersion,
          argumentsMap,
          eventType,
          maybeOriginalEventType,
          maybeScheduled = None, // This should be set once we have a way of retrieving scheduled behaviors in the API context
          isEphemeral = false,
          None,
          maybeTriggeringMessageId
        )
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

  def getFileFetchToken: Future[String] = Future.successful(botProfile.token)

  def uploadFile(file: File, filetype: Option[String], filename: Option[String]): Future[Option[String]] = {
    val client = slackApiService.clientFor(botProfile)
    client.uploadFile(Some(file), content = None, filetype, filename).map { file =>
      file.permalink
    }
  }

  def uploadContent(content: String, filetype: Option[String], filename: Option[String]): Future[Option[String]] = {
    val client = slackApiService.clientFor(botProfile)
    client.uploadFile(None, content = Some(content), filetype, filename).map { file =>
      file.permalink
    }
  }

  override def scheduleByName(
                               actionName: String,
                               info: ScheduleActionInfo
                             )(implicit request: Request[AnyContent]): Future[Result] = {
    for {
      maybeSlackChannelId <- maybeSlackChannelIdFor(Some(info.channel))
      maybeOriginatingBehaviorVersion <- maybeOriginatingBehaviorVersion
      maybeBehaviorVersion <- maybeBehaviorVersionFor(actionName, maybeOriginatingBehaviorVersion)
      result <- (for {
        behaviorVersion <- maybeBehaviorVersion
      } yield {
        for {
          user <- dataService.users.ensureUserFor(slackProfile.loginInfo, Seq(), behaviorVersion.team.id)
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
      maybeSlackChannelId <- maybeSlackChannelIdFor(Some(info.channel))
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
        maybeSlackChannelIdFor(Some(channel))
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
        maybeSlackChannelIdFor(Some(channel))
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
          user <- dataService.users.ensureUserFor(slackProfile.loginInfo, Seq(), behaviorVersion.team.id)
          listener <- dataService.messageListeners.ensureFor(
            behaviorVersion.behavior,
            info.argumentsMap,
            user,
            team,
            info.medium,
            info.channel,
            info.threadId,
            isForCopilot = info.isForCoPilot.contains(true)
          )
        } yield {
          Ok(listener.id)
        }
      }).getOrElse {
        Future.successful(responder.notFound(APIErrorData(s"Couldn't add listener for action `${info.actionName}`", Some("actionName")), Json.toJson(info)))
      }
    } yield result
  }

  override def disableMessageListener(
                                       info: DisableMessageListenerInfo
                                     )(implicit request: Request[AnyContent]): Future[Result] = {
    for {
      maybeOriginatingBehaviorVersion <- maybeOriginatingBehaviorVersion
      maybeBehaviorVersion <- maybeBehaviorVersionFor(info.actionName, maybeOriginatingBehaviorVersion)
      result <- (for {
        behaviorVersion <- maybeBehaviorVersion
        _ <- maybeTeam
      } yield {
        for {
          user <- dataService.users.ensureUserFor(slackProfile.loginInfo, Seq(), behaviorVersion.team.id)
          updatedListeners <- dataService.messageListeners.disableFor(
            behaviorVersion.behavior,
            user,
            info.medium,
            info.channel,
            info.threadId,
            isForCopilot = info.isForCoPilot.contains(true)
          )
        } yield {
          Ok(Json.toJson(updatedListeners))
        }
      }).getOrElse {
        Future.successful(responder.notFound(APIErrorData(s"Couldn't disable listener for action `${info.actionName}`", Some("actionName")), Json.toJson(info)))
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

