package models.behaviors.scheduledmessage

import models.team.Team
import org.joda.time.format.DateTimeFormat
import org.joda.time.DateTime
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.SimpleTextResult
import models.behaviors.events.{SlackMessageContext, SlackMessageEvent}
import services.{DataService, SlackService}
import slack.models.Message
import slack.rtm.SlackRtmClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class ScheduledMessage(
                             id: String,
                             text: String,
                             team: Team,
                             maybeChannelName: Option[String],
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

  def scheduleInfoResult = SimpleTextResult(
    s"""I've been asked to run `$text` ${recurrence.displayString.trim}.
        |
       |For more details on what is scheduled, try `@ellipsis: scheduled`.
        |
       |Here goes:
     """.stripMargin)

  def listResponse: String = {
    s"""`$text` ${recurrence.displayString.trim}
        |
       |$nextRunsString
     """.stripMargin
  }

  val nextRunDateFormatter = DateTimeFormat.forPattern("MMMM d, yyyy")
  def nextRunDateStringFor(when: DateTime): String = {
    val clarifier = if (when.toLocalDate == DateTime.now.toLocalDate) {
      " (today)"
    } else if (when.toLocalDate == DateTime.now.plusDays(1).toLocalDate) {
      " (tomorrow)"
    } else {
      ""
    }

    when.toString(nextRunDateFormatter) ++ clarifier
  }
  def nextRunTimeStringFor(when: DateTime): String = when.toString(Recurrence.timeFormatter)

  def nextRunStringFor(when: DateTime): String = s"${nextRunDateStringFor(when)} at ${nextRunTimeStringFor(when)}"

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

  // TODO: don't be slack-specific
  def send(slackService: SlackService, client: SlackRtmClient, profile: SlackBotProfile, dataService: DataService): Future[Unit] = {
    maybeChannelName.map { channelName =>
      val message = Message("ts", channelName, profile.userId, text, None)
      val context = SlackMessageContext(client, profile, message)
      for {
        results <- slackService.eventHandler.startInvokeConversationFor(SlackMessageEvent(context))
        _ <- dataService.scheduledMessages.save(withUpdatedNextTriggeredFor(DateTime.now))
      } yield {
        results.foreach { result =>
          if (result.hasText) {
            scheduleInfoResult.sendIn(context)
          }
          result.sendIn(context)
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
