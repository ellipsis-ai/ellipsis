package json

import models.behaviors.scheduling.recurrence.Recurrence

case class ScheduledActionRecurrenceTimeData(hour: Int, minute: Int)

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
