package models.behaviors.scheduling

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

import akka.actor.ActorSystem
import models.accounts.slack.botprofile.SlackBotProfile
import models.accounts.slack.profile.SlackProfile
import models.accounts.user.User
import models.behaviors.events.{EventHandler, ScheduledEvent}
import models.behaviors.scheduling.recurrence.Recurrence
import models.behaviors.{BotResult, SimpleTextResult}
import models.team.Team
import play.api.{Configuration, Logger}
import services.DataService
import slack.api.{ApiError, SlackApiClient}
import utils.SlackChannels

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait Scheduled {

  val id: String
  val maybeUser: Option[User]
  val team: Team
  val maybeChannelName: Option[String]
  val isForIndividualMembers: Boolean
  val recurrence: Recurrence
  val nextSentAt: OffsetDateTime
  val createdAt: OffsetDateTime

  def displayText(dataService: DataService): Future[String]

  def followingSentAt: OffsetDateTime = recurrence.nextAfter(nextSentAt)

  def successResponse(dataService: DataService): Future[String] = {
    shortDescription("OK, I will run", dataService)
  }

  def scheduleInfoResultFor(
                             event: ScheduledEvent,
                             result: BotResult,
                             configuration: Configuration,
                             didInterrupt: Boolean,
                             displayText: String
                           ): BotResult = {
    val helpLink = configuration.getString("application.apiBaseUrl").map { baseUrl =>
      val path = controllers.routes.HelpController.scheduledMessages()
      s"$baseUrl$path"
    }.get
    val resultText = if (didInterrupt) {
      s"""Meanwhile, I’m running $displayText [as scheduled]($helpLink) _(${recurrence.displayString.trim})._
         |
       |───
     """.stripMargin
    } else {
      s""":wave: Hi.
         |
       |I’m running $displayText [as scheduled]($helpLink) _(${recurrence.displayString.trim})._
         |
       |───
         |""".stripMargin
    }
    SimpleTextResult(event, resultText, result.forcePrivateResponse)
  }

  def isScheduledForDirectMessage: Boolean = {
    maybeChannelName.exists(_.startsWith("D"))
  }

  def isScheduledForPrivateChannel: Boolean = {
    maybeChannelName.exists(_.startsWith("G"))
  }

  def recurrenceAndChannel: String = {
    val channelInfo = maybeChannelName.map { channelName =>
      if (isScheduledForDirectMessage) {
        "in a direct message"
      } else if (isScheduledForPrivateChannel) {
        "in a private channel"
      } else if (isForIndividualMembers) {
        s"in a direct message to each member of <#$channelName>"
      } else {
        s"in <#$channelName>"
      }
    }.getOrElse("")
    s"${recurrence.displayString.trim} $channelInfo"
  }

  def shortDescription(prefix: String, dataService: DataService): Future[String] = {
    displayText(dataService).map { displayText =>
      s"$prefix $displayText $recurrenceAndChannel."
    }
  }

  def listResponse(dataService: DataService): Future[String] = {
    shortDescription("Run", dataService).map { desc =>
      s"""
         |
        |**$desc**
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

  def botProfile(dataService: DataService): Future[Option[SlackBotProfile]] = {
    dataService.slackBotProfiles.allFor(team).map(_.headOption)
  }

  def maybeSlackProfile(dataService: DataService): Future[Option[SlackProfile]] = {
    maybeUser.map { user =>
      for {
        maybeSlackLinkedAccount <- dataService.linkedAccounts.maybeForSlackFor(user)
        maybeSlackProfile <- maybeSlackLinkedAccount.map { linkedAccount =>
          dataService.slackProfiles.find(linkedAccount.loginInfo)
        }.getOrElse(Future.successful(None))
      } yield maybeSlackProfile
    }.getOrElse(Future.successful(None))
  }

  case class SlackDMInfo(userId: String, channelId: String)

  private def sendDMsSequentiallyFor(
                                      infos: List[SlackDMInfo],
                                      eventHandler: EventHandler,
                                      client: SlackApiClient,
                                      profile: SlackBotProfile,
                                      dataService: DataService,
                                      configuration: Configuration
                                    )(implicit actorSystem: ActorSystem): Future[Unit] = {
    if (infos.isEmpty) {
      Future.successful({})
    } else {
      val info = infos.head
      sendFor(info.channelId, info.userId, eventHandler, client, profile, dataService, configuration).flatMap { _ =>
        sendDMsSequentiallyFor(infos.tail, eventHandler, client, profile, dataService, configuration)
      }
    }
  }

  def sendForIndividualMembers(
                                channelName: String,
                                eventHandler: EventHandler,
                                client: SlackApiClient,
                                profile: SlackBotProfile,
                                dataService: DataService,
                                configuration: Configuration
                              )(implicit actorSystem: ActorSystem): Future[Unit] = {
    for {
      members <- SlackChannels(client).getMembersFor(channelName)
      otherMembers <- Future.successful(members.filterNot(ea => ea == profile.userId))
      dmInfos <- Future.sequence(otherMembers.map { ea =>
        client.openIm(ea).map { dmChannel =>
          Some(SlackDMInfo(ea, dmChannel))
        }.recover {
          case e: ApiError => {
            Logger.error(s"Couldn't send DM to $ea due to Slack API error: ${e.code}", e)
            None
          }
        }
      }).map(_.flatten)
      _ <- sendDMsSequentiallyFor(dmInfos.toList, eventHandler, client, profile, dataService, configuration)
    } yield {}
  }

  def eventFor(channelName: String, slackUserId: String, profile: SlackBotProfile): ScheduledEvent

  // TODO: don't be slack-specific
  def sendFor(
               channelName: String,
               slackUserId: String,
               eventHandler: EventHandler,
               client: SlackApiClient,
               profile: SlackBotProfile,
               dataService: DataService,
               configuration: Configuration
             )(implicit actorSystem: ActorSystem): Future[Unit] = {
    val event = eventFor(channelName, slackUserId, profile)
    for {
      didInterrupt <- eventHandler.interruptOngoingConversationsFor(event)
      results <- eventHandler.handle(event, None)
    } yield {
      sendResults(results.toList, event, configuration, didInterrupt, dataService)
    }
  }

  def sendResult(
                  result: BotResult,
                  event: ScheduledEvent,
                  configuration: Configuration,
                  didInterrupt: Boolean,
                  dataService: DataService
                )(implicit actorSystem: ActorSystem): Future[Unit] = {
    for {
      displayText <- displayText(dataService)
      _ <- if (result.hasText) {
        scheduleInfoResultFor(event, result, configuration, didInterrupt, displayText).sendIn(None, None, dataService)
      } else {
        Future.successful({})
      }
      _ <- result.sendIn(None, None, dataService)
    } yield {
      val channelInfo =
        event.maybeChannel.
          map { channel => s" in channel $channel" }.
          getOrElse("")
      Logger.info(s"Sending result [${result.fullText}] for scheduled message [$displayText]$channelInfo")
    }
  }

  def sendResults(
                   results: List[BotResult],
                   event: ScheduledEvent,
                   configuration: Configuration,
                   didInterrupt: Boolean,
                   dataService: DataService
                 )(implicit actorSystem: ActorSystem): Future[Unit] = {
    if (results.isEmpty) {
      Future.successful({})
    } else {
      sendResult(results.head, event, configuration, didInterrupt, dataService).flatMap { _ =>
        sendResults(results.tail, event, configuration, didInterrupt, dataService)
      }
    }
  }

  def send(
            eventHandler: EventHandler,
            client: SlackApiClient,
            profile: SlackBotProfile,
            dataService: DataService,
            configuration: Configuration
          )(implicit actorSystem: ActorSystem): Future[Unit] = {
    maybeChannelName.map { channelName =>
      if (isForIndividualMembers) {
        sendForIndividualMembers(channelName, eventHandler, client, profile, dataService, configuration)
      } else {
        maybeSlackProfile(dataService).flatMap { maybeSlackProfile =>
          val slackUserId = maybeSlackProfile.map(_.loginInfo.providerKey).getOrElse(profile.userId)
          sendFor(channelName, slackUserId, eventHandler, client, profile, dataService, configuration)
        }
      }
    }.getOrElse(Future.successful(Unit))
  }

  def updateNextTriggeredFor(dataService: DataService): Future[Scheduled]

}

object Scheduled {

  def allToBeSent(dataService: DataService): Future[Seq[Scheduled]] = {
    for {
      scheduledMessages <- dataService.scheduledMessages.allToBeSent
      scheduledBehaviors <- dataService.scheduledBehaviors.allToBeSent
    } yield scheduledMessages ++ scheduledBehaviors
  }

  def allForTeam(team: Team, dataService: DataService): Future[Seq[Scheduled]] = {
    for {
      scheduledMessages <- dataService.scheduledMessages.allForTeam(team)
      scheduledBehaviors <- dataService.scheduledBehaviors.allForTeam(team)
    } yield scheduledMessages ++ scheduledBehaviors
  }

}
