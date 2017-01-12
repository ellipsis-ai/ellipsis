package models.behaviors.scheduledmessage

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

import models.team.Team
import models.accounts.slack.botprofile.SlackBotProfile
import models.accounts.slack.profile.SlackProfile
import models.accounts.user.User
import models.behaviors.events.EventHandler
import models.behaviors.{BotResult, SimpleTextResult}
import play.api.{Configuration, Logger}
import services.slack.SlackMessageEvent
import services.DataService
import slack.api.{ApiError, SlackApiClient}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class ScheduledMessage(
                             id: String,
                             text: String,
                             maybeUser: Option[User],
                             team: Team,
                             maybeChannelName: Option[String],
                             isForIndividualMembers: Boolean,
                             recurrence: Recurrence,
                             nextSentAt: OffsetDateTime,
                             createdAt: OffsetDateTime
                           ) {

  def followingSentAt: OffsetDateTime = recurrence.nextAfter(nextSentAt)

  def successResponse: String = s"OK, I will run $listResponse"

  def scheduleInfoResultFor(result: BotResult, configuration: Configuration) = {
    val helpLink = configuration.getString("application.apiBaseUrl").map { baseUrl =>
      val path = controllers.routes.HelpController.scheduledMessages()
      s"$baseUrl$path"
    }.get
    SimpleTextResult(
      s""">:mantelpiece_clock: I’m running `$text` [as scheduled]($helpLink) (${recurrence.displayString.trim}):
     """.stripMargin, result.forcePrivateResponse)
  }

  def isScheduledForDirectMessage: Boolean = {
    maybeChannelName.exists(_.startsWith("D"))
  }

  def isScheduledForPrivateChannel: Boolean = {
    maybeChannelName.exists(_.startsWith("G"))
  }

  def shortDescription: String = {
    val channelInfo = maybeChannelName.map { channelName =>
      if (isScheduledForDirectMessage) {
        "in a direct message"
      } else if (isScheduledForPrivateChannel) {
        "in a private channel"
      } else if (isForIndividualMembers) {
        s"privately for everyone in <#$channelName>"
      } else {
        s"in <#$channelName>"
      }
    }.getOrElse("")
    s"${recurrence.displayString.trim} ($channelInfo)"
  }

  def listResponse: String = {
    s"""`$text` $shortDescription
        |
        |$nextRunsString
     """.stripMargin
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

  private def swallowingChannelNotFound[T](fn: () => Future[T]): Future[Option[T]] = {
    fn().map(Some(_)).recover {
      case e: ApiError => if (e.code == "channel_not_found") {
        None
      } else {
        throw e
      }
    }
  }

  private def getMembersFor(channelOrGroupId: String, client: SlackApiClient): Future[Seq[String]] = {
    for {
      maybeChannel <- swallowingChannelNotFound(() => client.getChannelInfo(channelOrGroupId))
      maybeGroup <- swallowingChannelNotFound(() => client.getGroupInfo(channelOrGroupId))
    } yield {
      maybeChannel.flatMap(_.members).orElse(maybeGroup.map(_.members)).getOrElse(Seq())
    }
  }

  def sendForIndividualMembers(
                                channelName: String,
                                eventHandler: EventHandler,
                                client: SlackApiClient,
                                profile: SlackBotProfile,
                                dataService: DataService,
                                configuration: Configuration
                              ): Future[Unit] = {
    for {
      members <- getMembersFor(channelName, client)
      otherMembers <- Future.successful(members.filterNot(ea => ea == profile.userId))
      withDMChannels <- Future.sequence(otherMembers.map { ea =>
        client.openIm(ea).map { dmChannel =>
          (ea, dmChannel)
        }
      })
      _ <- Future.sequence(withDMChannels.map { case(slackUserId, dmChannel) =>
        sendFor(dmChannel, slackUserId, eventHandler, client, profile, dataService, configuration)
      }).map(_ => {})
    } yield {}
  }

  // TODO: don't be slack-specific
  def sendFor(
               channelName: String,
               slackUserId: String,
               eventHandler: EventHandler,
               client: SlackApiClient,
               profile: SlackBotProfile,
               dataService: DataService,
               configuration: Configuration
             ): Future[Unit] = {
    for {
      event <- Future.successful(SlackMessageEvent(profile, channelName, slackUserId, text, "ts"))
      _ <- eventHandler.interruptOngoingConversationsFor(event)
      results <- eventHandler.handle(event, None)
    } yield {
      sendResults(results.toList, event, configuration)
    }
  }

  def sendResult(result: BotResult, event: SlackMessageEvent, configuration: Configuration): Future[Unit] = {
    for {
      _ <- if (result.hasText) {
        scheduleInfoResultFor(result, configuration).sendIn(event, None, None)
      } else {
        Future.successful({})
      }
      _ <- result.sendIn(event, None, None)
    } yield {
      Logger.info(s"Sending result [${result.fullText}] for scheduled message [${text}] in channel [${event.channel}]")
    }
  }

  def sendResults(results: List[BotResult], event: SlackMessageEvent, configuration: Configuration): Future[Unit] = {
    if (results.isEmpty) {
      Future.successful({})
    } else {
      sendResult(results.head, event, configuration).flatMap { _ =>
        sendResults(results.tail, event, configuration)
      }
    }
  }

  def send(
            eventHandler: EventHandler,
            client: SlackApiClient,
            profile: SlackBotProfile,
            dataService: DataService,
            configuration: Configuration
          ): Future[Unit] = {
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

  def withUpdatedNextTriggeredFor(when: OffsetDateTime): ScheduledMessage = {
    this.copy(nextSentAt = recurrence.nextAfter(when))
  }

  def toRaw: RawScheduledMessage = {
    RawScheduledMessage(
      RawScheduledMessageBase(
        id,
        text,
        maybeUser.map(_.id),
        team.id,
        maybeChannelName,
        isForIndividualMembers,
        recurrence.typeName,
        recurrence.frequency,
        nextSentAt,
        createdAt
      ),
      RawScheduledMessageOptions(
        recurrence.maybeTimeOfDay,
        recurrence.maybeTimeZone,
        recurrence.maybeMinuteOfHour,
        recurrence.maybeDayOfWeek.map(_.getValue),
        recurrence.maybeMonday,
        recurrence.maybeTuesday,
        recurrence.maybeWednesday,
        recurrence.maybeThursday,
        recurrence.maybeFriday,
        recurrence.maybeSaturday,
        recurrence.maybeSunday,
        recurrence.maybeDayOfMonth,
        recurrence.maybeNthDayOfWeek,
        recurrence.maybeMonth
      )
    )
  }
}
