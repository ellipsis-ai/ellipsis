package models.behaviors.scheduling

import java.sql.Timestamp
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

import akka.actor.ActorSystem
import drivers.SlickPostgresDriver.api._
import models.accounts.{BotContext, SlackContext}
import models.accounts.slack.botprofile.SlackBotProfile
import models.accounts.slack.profile.SlackProfile
import models.accounts.user.User
import models.behaviors.{AdminSkillErrorNotificationResult, BotResult}
import models.behaviors.events.{Event, EventHandler}
import models.behaviors.scheduling.recurrence.Recurrence
import models.team.Team
import play.api.{Configuration, Logger}
import services.slack.SlackApiError
import services.{DataService, DefaultServices}
import slick.dbio.DBIO
import utils.{FutureSequencer, SlackChannels, SlackMessageSenderChannelException}

import scala.concurrent.{ExecutionContext, Future}

trait Scheduled {

  val id: String
  val maybeUser: Option[User]
  val team: Team
  val maybeChannel: Option[String]
  val maybeBehaviorGroupId: Option[String]
  val isForIndividualMembers: Boolean
  val recurrence: Recurrence
  val nextSentAt: OffsetDateTime
  val createdAt: OffsetDateTime
  val context: BotContext = SlackContext // TODO: Implement scheduled behaviors for other contexts

  def displayText(dataService: DataService)(implicit ec: ExecutionContext): Future[String]

  def maybeFollowingSentAt: Option[OffsetDateTime] = {
    if (recurrence.shouldRunAgainAfterNextRun) {
      Some(recurrence.nextAfter(nextSentAt))
    } else {
      None
    }
  }

  def successResponse(dataService: DataService)(implicit ec: ExecutionContext): Future[String] = {
    displayText(dataService).map { displayText =>
      s"""OK, I will run $displayText $recurrenceAndChannel."""
    }
  }

  def editScheduleUrlFor(configuration: Configuration): String = {
    val baseUrl = configuration.get[String]("application.apiBaseUrl")
    val path = controllers.routes.ScheduledActionsController.index(Some(id), newSchedule = None, maybeChannel, skillId = None, Some(team.id), forceAdmin = None)
    baseUrl + path
  }

  def isScheduledForDirectMessage: Boolean = {
    maybeChannel.exists(_.startsWith("D"))
  }

  def isScheduledForPrivateChannel: Boolean = {
    maybeChannel.exists(_.startsWith("G"))
  }

  def couldSendWithBotProfile: Boolean = {
    !isScheduledForDirectMessage && !isForIndividualMembers
  }

  def recurrenceAndChannel: String = {
    val channelInfo = maybeChannel.map { channel =>
      if (isScheduledForDirectMessage) {
        "in a direct message"
      } else if (isScheduledForPrivateChannel) {
        "in a private channel"
      } else if (isForIndividualMembers) {
        s"in a direct message to each member of <#$channel>"
      } else {
        s"in <#$channel>"
      }
    }.getOrElse("")
    s"${recurrence.displayString.trim} $channelInfo"
  }

  def recipientDetails: String = {
    if (isForIndividualMembers) {
      " in a direct message to each member of the channel"
    } else {
      ""
    }
  }

  def listResponse(
                    scheduleId: String,
                    teamId: String,
                    dataService: DataService,
                    configuration: Configuration,
                    includeChannel: Boolean
                  )(implicit ec: ExecutionContext): Future[String] = {
    val details = if (includeChannel) {
      recurrenceAndChannel
    } else {
      recurrence.displayString.trim ++ recipientDetails
    }
    val scheduleLink = s"_[✎ Edit](${editScheduleUrlFor(configuration)})_"
    displayText(dataService).map { desc =>
      s"""
        |
        |**Run $desc $details.** $scheduleLink
        |
        |$nextRunsString
        |
        |
     """.stripMargin
    }
  }

  val nextRunDateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")
  def nextRunDateStringFor(when: OffsetDateTime): String = {
    if (when.toLocalDate == OffsetDateTime.now.toLocalDate) {
      "Today"
    } else if (when.toLocalDate == OffsetDateTime.now.plusDays(1).toLocalDate) {
      "Tomorrow"
    } else {
      nextRunDateFormatter.format(when)
    }
  }
  def nextRunTimeStringFor(when: OffsetDateTime): String = {
    Recurrence.timeFormatterWithZone.format(when.toZonedDateTime.withZoneSameInstant(team.timeZone))
  }

  def nextRunStringFor(when: OffsetDateTime): String = {
    val whenInDefaultTimeZone = Recurrence.withZone(when, team.timeZone)
    s"${nextRunDateStringFor(whenInDefaultTimeZone)} at ${nextRunTimeStringFor(whenInDefaultTimeZone)}"
  }

  def nextRunsString: String = {
    maybeFollowingSentAt.map { followingSentAt =>
      s"""The next two times will be:
         | - ${nextRunStringFor(nextSentAt)}
         | - ${nextRunStringFor(followingSentAt)}
         |
       """.stripMargin
    }.getOrElse {
      s"""The next time will be:
         | - ${nextRunStringFor(nextSentAt)}
       """.stripMargin
    }
  }

  def botProfileAction(dataService: DataService)(implicit ec: ExecutionContext): DBIO[Option[SlackBotProfile]] = {
    dataService.slackBotProfiles.allForAction(team).map(_.headOption)
  }

  def maybeSlackProfile(dataService: DataService)(implicit ec: ExecutionContext): Future[Option[SlackProfile]] = {
    maybeUser.map { user =>
      dataService.users.maybeSlackProfileFor(user)
    }.getOrElse(Future.successful(None))
  }

  case class SlackDMInfo(userId: String, teamId: String, channelId: String)

  def sendForIndividualMembers(
                                channel: String,
                                eventHandler: EventHandler,
                                profile: SlackBotProfile,
                                services: DefaultServices
                              )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Unit] = {
    for {
      memberIds <- SlackChannels(services.slackApiService.clientFor(profile)).getMembersFor(channel)
      _ <- FutureSequencer.sequence(memberIds, sendForFn(eventHandler, profile, services))
    } yield {}
  }

  def eventFor(channel: String, slackUserId: String, profile: SlackBotProfile, services: DefaultServices)(implicit ec: ExecutionContext): Future[Option[Event]]

  // TODO: don't be slack-specific
  def sendFor(
               channel: String,
               slackUserId: String,
               eventHandler: EventHandler,
               profile: SlackBotProfile,
               services: DefaultServices
             )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Unit] = {
    for {
      maybeEvent <- eventFor(channel, slackUserId, profile, services)
      _ <- maybeEvent.map { event =>
        eventHandler.handle(event, None).flatMap { results =>
          FutureSequencer.sequence(results, sendResultFn(event, slackUserId, profile, services))
        }
      }.getOrElse(Future.successful(Seq()))
    } yield {}
  }

  def sendForFn(
                  eventHandler: EventHandler,
                  profile: SlackBotProfile,
                  services: DefaultServices
               )(implicit actorSystem: ActorSystem, ec: ExecutionContext): String => Future[Unit] = {
    slackUserId: String => {
      val client = services.slackApiService.clientFor(profile)
      for {
        maybeSlackUserData <- services.slackEventService.maybeSlackUserDataFor(slackUserId, client, (e) => {
          Logger.error(
            s"""Slack API reported user not found while trying to send a scheduled message:
               |Slack user ID: $slackUserId
               |Ellipsis bot Slack team ID: ${profile.slackTeamId}
               |Ellipsis user ID: ${maybeUser.map(_.id).getOrElse("unknown")}
               |Ellipsis team ID: ${team.id}
             """.stripMargin, e)
          None
        })
        maybeDmInfo <- maybeSlackUserData.filter { userData =>
          userData.accountId != profile.userId && !userData.deleted && !userData.isBot
        }.map { userData =>
          services.slackApiService.clientFor(profile).openConversationFor(userData.accountId).map { dmChannel =>
            Some(SlackDMInfo(userData.accountId, userData.firstTeamId, dmChannel))
          }.recover {
            case e: SlackApiError => {
              val msg = s"""Couldn't open DM for scheduled message to @${userData.getDisplayName} (${userData.accountId}) on Slack team ${userData.firstTeamId} due to Slack API error: ${e.code}"""
              Logger.error(msg, e)
              None
            }
          }
        }.getOrElse(Future.successful(None))
        _ <- maybeDmInfo.map { info =>
          sendFor(info.channelId, info.userId, eventHandler, profile, services)
        }.getOrElse(Future.successful({}))
      } yield {}
    }
  }

  def sendResult(
                  result: BotResult,
                  event: Event,
                  services: DefaultServices
                )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Unit] = {
    val dataService = services.dataService
    val botResultService = services.botResultService
    for {
      displayText <- displayText(dataService)
      _ <- botResultService.sendIn(result, None)
    } yield {
      Logger.info(event.logTextFor(result, Some(s"for scheduled message [$displayText]")))
    }
  }

  def sendResultFn(
                    event: Event,
                    slackUserId: String,
                    profile: SlackBotProfile,
                    services: DefaultServices
                  )(implicit actorSystem: ActorSystem, ec: ExecutionContext): BotResult => Future[Unit] = {
    result: BotResult => {
      sendResult(result, event, services).recover {
        case c: SlackMessageSenderChannelException => {
          if (slackUserId != profile.userId) {
            val editLink = s"[✎ Edit schedule](${editScheduleUrlFor(services.configuration)})"
            for {
              scheduledDisplayText <- displayText(services.dataService)
              isAdminUserOnOtherTeam <- maybeUser.map { user =>
                services.dataService.users.isAdmin(user).map { isAdmin =>
                  isAdmin && user.teamId != event.ellipsisTeamId
                }
              }.getOrElse(Future.successful(false))
              maybeAdminTeamEvent <- if (isAdminUserOnOtherTeam) {
                services.dataService.slackBotProfiles.eventualMaybeManagedSkillErrorEvent(event.originalEventType)
              } else {
                Future.successful(None)
              }
              result <- {
                maybeAdminTeamEvent.map { adminEvent =>
                  val message = s"Unable to run ${scheduledDisplayText} on schedule. ${c.rawChannelReason}"
                  services.botResultService.sendIn(AdminSkillErrorNotificationResult(services.configuration, adminEvent, result, Some(message)), None)
                }.getOrElse {
                  val message =
                    s"""**I was unable to run ${scheduledDisplayText} on schedule for you in the specified channel.** ${c.formattedChannelReason}
                       |
                       |${editLink}
                       |""".stripMargin
                  services.dataService.slackBotProfiles.sendDMWarningMessageFor(event, services, profile, slackUserId, message)
                }
              }
            } yield result
          } else {
            throw c
          }
        }
      }
    }
  }

  def send(
            eventHandler: EventHandler,
            profile: SlackBotProfile,
            services: DefaultServices,
            scheduledDisplayText: String
          )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Unit] = {
    val dataService = services.dataService
    maybeChannel.map { channel =>
      if (isForIndividualMembers) {
        sendForIndividualMembers(channel, eventHandler, profile, services)
      } else {
        maybeSlackProfile(dataService).flatMap { maybeSlackProfile =>
          val maybeSlackUserId = maybeSlackProfile.map(_.loginInfo.providerKey)
          if (maybeSlackUserId.isDefined || couldSendWithBotProfile) {
            val slackUserId = maybeSlackUserId.getOrElse(profile.userId)
            sendFor(channel, slackUserId, eventHandler, profile, services)
          } else {
            val userId = maybeUser.map(_.id).getOrElse("(unknown)")
            Logger.warn(
              s"""Unable to run scheduled item for user ID ${userId} on team ${team.id} because no Slack profile was found and this was scheduled in a direct message.
                 |
                 |Scheduled ID: ${id}
                 |Summary: ${scheduledDisplayText}
                 |Channel: ${maybeChannel.getOrElse("(none)")}
                 |Recurrence: ${recurrence.displayString}
                 |""".stripMargin)
            Future.successful({})
          }
        }
      }
    }.getOrElse(Future.successful(Unit))
  }

  def updateOrDeleteScheduleAction(dataService: DataService)(implicit ec: ExecutionContext): DBIO[Option[Scheduled]] = {
    if (recurrence.shouldRunAgainAfterNextRun) {
      updateForNextRunAction(dataService).map(Some(_))
    } else {
      deleteAction(dataService).map(_ => None)
    }
  }

  def updateForNextRunAction(dataService: DataService): DBIO[Scheduled]

  def deleteAction(dataService: DataService)(implicit ec: ExecutionContext): DBIO[Unit]
}

object Scheduled {

  def allActiveForTeam(team: Team, dataService: DataService)(implicit ec: ExecutionContext): Future[Seq[Scheduled]] = {
    for {
      scheduledMessages <- dataService.scheduledMessages.allForTeam(team)
      scheduledBehaviors <- dataService.scheduledBehaviors.allActiveForTeam(team)
    } yield scheduledMessages ++ scheduledBehaviors
  }

  def allActiveForChannel(team: Team, channel: String, dataService: DataService)(implicit ec: ExecutionContext): Future[Seq[Scheduled]] = {
    for {
      scheduledMessages <- dataService.scheduledMessages.allForChannel(team, channel)
      scheduledBehaviors <- dataService.scheduledBehaviors.allActiveForChannel(team, channel)
    } yield scheduledMessages ++ scheduledBehaviors
  }

  def maybeNextToBeSentAction(when: OffsetDateTime, dataService: DataService)(implicit ec: ExecutionContext): DBIO[Option[Scheduled]] = {
    dataService.scheduledMessages.maybeNextToBeSentAction(when).flatMap { maybeNext =>
      maybeNext.map { next =>
        DBIO.successful(Some(next))
      }.getOrElse {
        dataService.scheduledBehaviors.maybeNextToBeSentAction(when)
      }
    }
  }

  def nextToBeSentIdQueryFor(tableName: String, when: OffsetDateTime): DBIO[Seq[String]] = {
    val ts = Timestamp.from(when.toInstant)
    sql"""
         SELECT id from #$tableName
         WHERE next_sent_at <= ${ts}
         ORDER BY id
         FOR UPDATE SKIP LOCKED
         LIMIT 1
         """.as[String]
  }

}
