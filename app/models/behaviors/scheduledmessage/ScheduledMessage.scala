package models.behaviors.scheduledmessage

import models.team.Team
import org.joda.time.format.DateTimeFormat
import org.joda.time.DateTime
import models.accounts.slack.botprofile.SlackBotProfile
import models.accounts.slack.profile.SlackProfile
import models.accounts.user.User
import models.behaviors.events.EventHandler
import models.behaviors.{BotResult, SimpleTextResult}
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
                             nextSentAt: DateTime,
                             createdAt: DateTime
                           ) {

  def followingSentAt: DateTime = recurrence.nextAfter(nextSentAt)

  def successResponse: String = {
    s"""OK, I will run `$text` ${recurrence.displayString.trim}.
        |
       |$nextRunsString
     """.stripMargin
  }

  def scheduleInfoResultFor(result: BotResult) = SimpleTextResult(
    s"""I've been asked to run `$text` ${recurrence.displayString.trim}.
        |
       |For more details on what is scheduled, try `@ellipsis: scheduled`.
        |
       |Here goes:
     """.stripMargin, result.forcePrivateResponse)

  def listResponse: String = {
    s"""`$text` ${recurrence.displayString.trim}
        |
       |$nextRunsString
     """.stripMargin
  }

  val nextRunDateFormatter = DateTimeFormat.forPattern("MMMM d, yyyy")
  def nextRunDateStringFor(when: DateTime): String = {
    val clarifier = if (when.toDate == DateTime.now.toDate) {
      " (today)"
    } else if (when.toDate == DateTime.now.plusDays(1).toDate) {
      " (tomorrow)"
    } else {
      ""
    }

    when.toString(nextRunDateFormatter) ++ clarifier
  }
  def nextRunTimeStringFor(when: DateTime): String = when.toString(Recurrence.timeFormatterWithZone)

  def nextRunStringFor(when: DateTime): String = {
    val whenInDefaultTimeZone = when.withZone(team.timeZone)
    s"${nextRunDateStringFor(whenInDefaultTimeZone)} at ${nextRunTimeStringFor(whenInDefaultTimeZone)}"
  }

  def nextRunsString: String = {
    s"""The next two runs will be:
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
                                dataService: DataService
                              ): Future[Unit] = {
    for {
      members <- getMembersFor(channelName, client)
      otherMembers <- Future.successful(members.filterNot(ea => ea == profile.userId))
      withDMChannels <- Future.sequence(otherMembers.map { ea =>
        client.listIms.map { ims =>
          ims.find(_.user == ea).map(_.id).map { dmChannel =>
            (ea, dmChannel)
          }
        }
      }).map(_.flatten)
      _ <- Future.sequence(withDMChannels.map { case(slackUserId, dmChannel) =>
        sendFor(dmChannel, slackUserId, eventHandler, client, profile, dataService)
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
               dataService: DataService
             ): Future[Unit] = {
    for {
      event <- Future.successful(SlackMessageEvent(profile, channelName, slackUserId, text, "ts"))
      _ <- eventHandler.interruptOngoingConversationsFor(event)
      results <- eventHandler.handle(event, None)
      _ <- dataService.scheduledMessages.save(withUpdatedNextTriggeredFor(DateTime.now))
    } yield {
      results.foreach { result =>
        if (result.hasText) {
          scheduleInfoResultFor(result).sendIn(event, None, None)
        }
        result.sendIn(event, None, None)
      }
    }
  }

  def send(eventHandler: EventHandler, client: SlackApiClient, profile: SlackBotProfile, dataService: DataService): Future[Unit] = {
    maybeChannelName.map { channelName =>
      if (isForIndividualMembers) {
        sendForIndividualMembers(channelName, eventHandler, client, profile, dataService)
      } else {
        maybeSlackProfile(dataService).flatMap { maybeSlackProfile =>
          val slackUserId = maybeSlackProfile.map(_.loginInfo.providerKey).getOrElse(profile.userId)
          sendFor(channelName, slackUserId, eventHandler, client, profile, dataService)
        }
      }
    }.getOrElse(Future.successful(Unit))
  }

  def withUpdatedNextTriggeredFor(when: DateTime): ScheduledMessage = {
    this.copy(nextSentAt = recurrence.nextAfter(when))
  }

  def toRaw: RawScheduledMessage = {
    RawScheduledMessage(
      id,
      text,
      maybeUser.map(_.id),
      team.id,
      maybeChannelName,
      isForIndividualMembers,
      recurrence.typeName,
      recurrence.frequency,
      recurrence.maybeTimeOfDay,
      recurrence.maybeTimeZone,
      recurrence.maybeMinuteOfHour,
      recurrence.maybeDayOfWeek,
      recurrence.maybeDayOfMonth,
      recurrence.maybeNthDayOfWeek,
      recurrence.maybeMonth,
      nextSentAt,
      createdAt
    )
  }
}
