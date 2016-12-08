package models.behaviors.scheduledmessage

import models.team.Team
import org.joda.time.format.DateTimeFormat
import org.joda.time.LocalDateTime
import models.accounts.slack.botprofile.SlackBotProfile
import models.accounts.slack.profile.SlackProfile
import models.accounts.user.User
import models.behaviors.{BotResult, SimpleTextResult}
import models.behaviors.events.{SlackMessageContext, SlackMessageEvent}
import services.{DataService, SlackService}
import slack.models.Message
import slack.rtm.SlackRtmClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class ScheduledMessage(
                             id: String,
                             text: String,
                             maybeUser: Option[User],
                             team: Team,
                             maybeChannelName: Option[String],
                             recurrence: Recurrence,
                             nextSentAt: LocalDateTime,
                             createdAt: LocalDateTime
                           ) {

  def followingSentAt: LocalDateTime = recurrence.nextAfter(nextSentAt)

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
  def nextRunDateStringFor(when: LocalDateTime): String = {
    val clarifier = if (when.toLocalDate == LocalDateTime.now.toLocalDate) {
      " (today)"
    } else if (when.toLocalDate == LocalDateTime.now.plusDays(1).toLocalDate) {
      " (tomorrow)"
    } else {
      ""
    }

    when.toString(nextRunDateFormatter) ++ clarifier
  }
  def nextRunTimeStringFor(when: LocalDateTime): String = when.toString(Recurrence.timeFormatter)

  def nextRunStringFor(when: LocalDateTime): String = s"${nextRunDateStringFor(when)} at ${nextRunTimeStringFor(when)}"

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

  // TODO: don't be slack-specific
  def send(slackService: SlackService, client: SlackRtmClient, profile: SlackBotProfile, dataService: DataService): Future[Unit] = {
    maybeChannelName.map { channelName =>
      for {
        maybeSlackProfile <- maybeSlackProfile(dataService)
        slackUserId <- Future.successful(maybeSlackProfile.map(_.loginInfo.providerKey).getOrElse(profile.userId))
        message <- Future.successful(Message("ts", channelName, slackUserId, text, None))
        context <- Future.successful(SlackMessageContext(client, profile, message))
        results <- slackService.eventHandler.startInvokeConversationFor(SlackMessageEvent(context))
        _ <- dataService.scheduledMessages.save(withUpdatedNextTriggeredFor(LocalDateTime.now))
      } yield {
        results.foreach { result =>
          if (result.hasText) {
            scheduleInfoResultFor(result).sendIn(context, None, None)
          }
          result.sendIn(context, None, None)
        }
      }
    }.getOrElse(Future.successful(Unit))
  }

  def withUpdatedNextTriggeredFor(when: LocalDateTime): ScheduledMessage = {
    this.copy(nextSentAt = recurrence.nextAfter(when))
  }

  def toRaw: RawScheduledMessage = {
    RawScheduledMessage(
      id,
      text,
      maybeUser.map(_.id),
      team.id,
      maybeChannelName,
      recurrence.typeName,
      recurrence.frequency,
      recurrence.maybeTimeOfDay,
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
