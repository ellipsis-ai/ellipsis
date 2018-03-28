package models.behaviors.scheduling

import java.sql.Timestamp
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

import akka.actor.ActorSystem
import drivers.SlickPostgresDriver.api._
import models.accounts.slack.botprofile.SlackBotProfile
import models.accounts.slack.profile.SlackProfile
import models.accounts.user.User
import models.behaviors.BotResult
import models.behaviors.events.{EventHandler, ScheduledEvent}
import models.behaviors.scheduling.recurrence.Recurrence
import models.team.Team
import play.api.{Configuration, Logger}
import services.{DataService, DefaultServices}
import slack.api.{ApiError, SlackApiClient}
import slick.dbio.DBIO
import utils.{FutureSequencer, SlackChannels}

import scala.concurrent.{ExecutionContext, Future}

trait Scheduled {

  val id: String
  val maybeUser: Option[User]
  val team: Team
  val maybeChannel: Option[String]
  val isForIndividualMembers: Boolean
  val recurrence: Recurrence
  val nextSentAt: OffsetDateTime
  val createdAt: OffsetDateTime

  def displayText(dataService: DataService)(implicit ec: ExecutionContext): Future[String]

  def followingSentAt: OffsetDateTime = recurrence.nextAfter(nextSentAt)

  def successResponse(dataService: DataService)(implicit ec: ExecutionContext): Future[String] = {
    displayText(dataService).map { displayText =>
      s"""OK, I will run $displayText $recurrenceAndChannel."""
    }
  }

  def maybeScheduleInfoTextFor(
                           event: ScheduledEvent,
                           result: BotResult,
                           configuration: Configuration,
                           displayText: String,
                           isForInterruption: Boolean
                         ): Option[String] = {
    if (result.hasText) {
      val scheduleLink = scheduleLinkFor(configuration, event.scheduled.id, event.teamId)
      val greeting = if (isForInterruption) {
        "Meanwhile, "
      } else {
        """:wave: Hi.
         |
         |""".stripMargin
      }
      Some(s"""${greeting}I’m running $displayText as scheduled. $scheduleLink
       |
       |───
       |
       |""".stripMargin)
    } else {
      None
    }
  }

  def scheduleLinkFor(configuration: Configuration, scheduleId: String, teamId: String): String = {
    configuration.getOptional[String]("application.apiBaseUrl").map { baseUrl =>
      val path = controllers.routes.ScheduledActionsController.index(Some(scheduleId), None, Some(teamId))
      s"_[✎ Edit]($baseUrl$path)_"
    }.getOrElse("")
  }

  def isScheduledForDirectMessage: Boolean = {
    maybeChannel.exists(_.startsWith("D"))
  }

  def isScheduledForPrivateChannel: Boolean = {
    maybeChannel.exists(_.startsWith("G"))
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
    val scheduleLink = scheduleLinkFor(configuration, scheduleId, teamId)
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
    s"""The next two times will be:
       | - ${nextRunStringFor(nextSentAt)}
       | - ${nextRunStringFor(followingSentAt)}
       |
     """.stripMargin
  }

  def botProfileAction(dataService: DataService)(implicit ec: ExecutionContext): DBIO[Option[SlackBotProfile]] = {
    dataService.slackBotProfiles.allForAction(team).map(_.headOption)
  }

  def maybeSlackProfile(dataService: DataService)(implicit ec: ExecutionContext): Future[Option[SlackProfile]] = {
    maybeUser.map { user =>
      for {
        maybeSlackLinkedAccount <- dataService.linkedAccounts.maybeForSlackFor(user)
        maybeSlackProfile <- maybeSlackLinkedAccount.map { linkedAccount =>
          dataService.slackProfiles.find(linkedAccount.loginInfo)
        }.getOrElse(Future.successful(None))
      } yield maybeSlackProfile
    }.getOrElse(Future.successful(None))
  }

  case class SlackDMInfo(userId: String, teamId: String, channelId: String)

  def sendForIndividualMembers(
                                channel: String,
                                eventHandler: EventHandler,
                                client: SlackApiClient,
                                profile: SlackBotProfile,
                                services: DefaultServices
                              )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Unit] = {
    for {
      memberIds <- SlackChannels(client, services.cacheService, profile.slackTeamId).getMembersFor(channel)
      _ <- FutureSequencer.sequence(memberIds, sendForFn(eventHandler, client, profile, services))
    } yield {}
  }

  def eventFor(channel: String, slackUserId: String, slackTeamId: String, profile: SlackBotProfile, client: SlackApiClient): ScheduledEvent

  // TODO: don't be slack-specific
  def sendFor(
               channel: String,
               slackUserId: String,
               slackTeamId: String,
               eventHandler: EventHandler,
               client: SlackApiClient,
               profile: SlackBotProfile,
               services: DefaultServices
             )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Unit] = {
    val event = eventFor(channel, slackUserId, slackTeamId, profile, client)
    for {
      results <- eventHandler.handle(event, None)
    } yield {
      FutureSequencer.sequence(results, sendResultFn(event, services))
    }
  }

  def sendForFn(
                  eventHandler: EventHandler,
                  client: SlackApiClient,
                  profile: SlackBotProfile,
                  services: DefaultServices
               )(implicit actorSystem: ActorSystem, ec: ExecutionContext): String => Future[Unit] = {
    slackUserId: String => {
      for {
        maybeSlackUserData <- services.slackEventService.maybeSlackUserDataFor(slackUserId, profile.slackTeamId, client)
        maybeDmInfo <- maybeSlackUserData.filter { userData =>
          userData.accountId != profile.userId && !userData.deleted
        }.map { userData =>
          client.openIm(userData.accountId).map { dmChannel =>
            Some(SlackDMInfo(userData.accountId, userData.accountTeamId, dmChannel))
          }.recover {
            case e: ApiError => {
              val msg = s"""Couldn't open DM for scheduled message to @${userData.getDisplayName} (${userData.accountId}) on Slack team ${userData.accountTeamId} due to Slack API error: ${e.code}"""
              Logger.error(msg, e)
              None
            }
          }
        }.getOrElse(Future.successful(None))
        _ <- maybeDmInfo.map { info =>
          sendFor(info.channelId, info.userId, info.teamId, eventHandler, client, profile, services)
        }.getOrElse(Future.successful({}))
      } yield {}
    }
  }

  def sendResult(
                  result: BotResult,
                  event: ScheduledEvent,
                  services: DefaultServices
                )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Unit] = {
    val dataService = services.dataService
    val configuration = services.configuration
    val botResultService = services.botResultService
    for {
      displayText <- displayText(dataService)
      maybeIntroText <- Future.successful(maybeScheduleInfoTextFor(event, result, configuration, displayText, isForInterruption = false))
      maybeInterruptionIntroText <- Future.successful(maybeScheduleInfoTextFor(event, result, configuration, displayText, isForInterruption = true))
      _ <- botResultService.sendIn(result, None, maybeIntroText, maybeInterruptionIntroText)
    } yield {
      val channelInfo =
        event.maybeChannel.
          map { channel => s" in channel $channel" }.
          getOrElse("")
      Logger.info(event.logTextFor(result, Some(s"for scheduled message [$displayText]")))
    }
  }

  def sendResultFn(
                    event: ScheduledEvent,
                    services: DefaultServices
                  )(implicit actorSystem: ActorSystem, ec: ExecutionContext): BotResult => Future[Unit] = {
    result: BotResult => sendResult(result, event, services)
  }

  def send(
            eventHandler: EventHandler,
            client: SlackApiClient,
            profile: SlackBotProfile,
            services: DefaultServices
          )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Unit] = {
    maybeChannel.map { channel =>
      if (isForIndividualMembers) {
        sendForIndividualMembers(channel, eventHandler, client, profile, services)
      } else {
        maybeSlackProfile(services.dataService).flatMap { maybeSlackProfile =>
          val slackUserId = maybeSlackProfile.map(_.loginInfo.providerKey).getOrElse(profile.userId)
          val slackTeamId = maybeSlackProfile.map(_.teamId).getOrElse(profile.slackTeamId)
          sendFor(channel, slackUserId, slackTeamId, eventHandler, client, profile, services)
        }
      }
    }.getOrElse(Future.successful(Unit))
  }

  def updateNextTriggeredForAction(dataService: DataService): DBIO[Scheduled]

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
