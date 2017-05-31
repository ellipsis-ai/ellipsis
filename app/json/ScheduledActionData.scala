package json

import java.time.{DayOfWeek, LocalTime, OffsetDateTime, ZoneId}

import models.behaviors.scheduling.recurrence.Recurrence
import models.behaviors.scheduling.scheduledbehavior.ScheduledBehavior
import models.behaviors.scheduling.scheduledmessage.ScheduledMessage
import services.DataService
import utils._

import scala.concurrent.{ExecutionContext, Future}

case class ScheduledActionRecurrenceTimeData(hour: Int, minute: Int)

case class ScheduledActionArgumentData(name: String, value: String)
case class ScheduledActionRecurrenceData(
  displayString: String,
  frequency: Int,
  typeName: String,
  timeOfDay: Option[ScheduledActionRecurrenceTimeData],
  timeZone: Option[String],
  minuteOfHour: Option[Int],
  dayOfWeek: Option[Int],
  dayOfMonth: Option[Int],
  nthDayOfWeek: Option[Int],
  month: Option[Int],
  daysOfWeek: Seq[Int]
)

object ScheduledActionRecurrenceData {
  def fromRecurrence(recurrence: Recurrence): ScheduledActionRecurrenceData = {
    ScheduledActionRecurrenceData(
      recurrence.displayString,
      recurrence.frequency,
      recurrence.typeName,
      recurrence.maybeTimeOfDay.map(time => ScheduledActionRecurrenceTimeData(time.getHour, time.getMinute)),
      recurrence.maybeTimeZone.map(_.toString),
      recurrence.maybeMinuteOfHour,
      recurrence.maybeDayOfWeek.map(_.getValue),
      recurrence.maybeDayOfMonth,
      recurrence.maybeNthDayOfWeek,
      recurrence.maybeMonth,
      recurrence.daysOfWeek.map(_.getValue)
    )
  }
}

case class ScheduledActionData(
  actionName: Option[String],
  trigger: Option[String],
  arguments: Seq[ScheduledActionArgumentData],
  recurrence: ScheduledActionRecurrenceData,
  firstRecurrence: OffsetDateTime,
  secondRecurrence: OffsetDateTime,
  useDM: Boolean,
  channel: String
)

case class ScheduledActionsData(teamId: String, scheduledActions: Seq[ScheduledActionData])

object ScheduledActionsData {
  private def nameForChannel(maybeChannel: Option[String], maybeChannelInfo: Option[Seq[ChannelLike]]): String = {
    (for {
      channel <- maybeChannel
      channelInfo <- maybeChannelInfo
      matchingChannel <- channelInfo.find(ea => ea.id == channel || ea.name == channel)
    } yield {
      matchingChannel match {
        case SlackChannel(namedChannel) => s"""#${namedChannel.name}"""
        case SlackGroup(_) => "Private group"
        case SlackDM(_) => "Direct message"
      }
    }).getOrElse("Unknown")
  }

  def fromScheduleData(teamId: String, dataService: DataService, maybeChannelInfo: Option[Seq[ChannelLike]], scheduledMessages: Seq[ScheduledMessage], scheduledBehaviors: Seq[ScheduledBehavior])(implicit ec: ExecutionContext): Future[ScheduledActionsData] = {

    val fromMessages = scheduledMessages.map { ea =>
      Future.successful(
        ScheduledActionData(
          None,
          Some(ea.text),
          Seq(),
          ScheduledActionRecurrenceData.fromRecurrence(ea.recurrence),
          ea.nextSentAt,
          ea.followingSentAt,
          ea.isForIndividualMembers,
          nameForChannel(ea.maybeChannel, maybeChannelInfo)
        )
      )
    }

    val fromBehaviors = scheduledBehaviors.map { ea =>
      ea.displayText(dataService).map { behaviorName =>
        val arguments = ea.arguments.map { case(key, value) => ScheduledActionArgumentData(key, value) }.toSeq
        ScheduledActionData(
          Some(behaviorName),
          None,
          arguments,
          ScheduledActionRecurrenceData.fromRecurrence(ea.recurrence),
          ea.nextSentAt,
          ea.followingSentAt,
          ea.isForIndividualMembers,
          nameForChannel(ea.maybeChannel, maybeChannelInfo)
        )
      }
    }

    for {
      messages <- Future.sequence(fromMessages)
      behaviors <- Future.sequence(fromBehaviors)
    } yield {
      ScheduledActionsData(teamId, messages ++ behaviors)
    }
  }
}
