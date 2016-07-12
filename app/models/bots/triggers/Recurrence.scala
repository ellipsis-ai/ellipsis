package models.bots.triggers

import org.joda.time.{MonthDay, LocalTime, DateTime}

sealed trait Recurrence {
  val frequency: Int
  def nextAfter(previous: DateTime): DateTime
}
case class Hourly(frequency: Int, minuteOfHour: Int) extends Recurrence {

  def nextAfter(previous: DateTime): DateTime = {
    previous.plusHours(frequency).withMinuteOfHour(minuteOfHour)
  }

}

object Hourly {
  val recurrenceType = "hourly"
}

case class Daily(frequency: Int, timeOfDay: LocalTime) extends Recurrence {

  def nextAfter(previous: DateTime): DateTime = {
    previous.plusDays(frequency).withTime(timeOfDay)
  }

}

object Daily {
  val recurrenceType = "daily"
}

case class Weekly(frequency: Int, dayOfWeek: Int, timeOfDay: LocalTime) extends Recurrence {

  def nextAfter(previous: DateTime): DateTime = {
    previous.plusWeeks(frequency).withDayOfWeek(dayOfWeek).withTime(timeOfDay)
  }

}

object Weekly {
  val recurrenceType = "weekly"
}

case class MonthlyByDayOfMonth(frequency: Int, dayOfMonth: Int, timeOfDay: LocalTime) extends Recurrence {

  def nextAfter(previous: DateTime): DateTime = {
    previous.plusMonths(frequency).withDayOfMonth(dayOfMonth).withTime(timeOfDay)
  }

}

object MonthlyByDayOfMonth {
  val recurrenceType = "monthly_by_day_of_month"
}

case class MonthlyByNthDayOfWeek(frequency: Int, dayOfWeek: Int, nth: Int, timeOfDay: LocalTime) extends Recurrence {

  def nextAfter(previous: DateTime): DateTime = {
    val firstOfTheMonth = previous.plusMonths(frequency).withDayOfMonth(1)
    val weeksToAdd = nth - 1
    val withTheDay = if (firstOfTheMonth.getDayOfWeek == dayOfWeek) {
      firstOfTheMonth.plusWeeks(weeksToAdd)
    } else if (firstOfTheMonth.getDayOfWeek < dayOfWeek) {
      firstOfTheMonth.withDayOfWeek(dayOfWeek).plusWeeks(weeksToAdd)
    } else {
      firstOfTheMonth.plusWeeks(1).withDayOfWeek(dayOfWeek).plusWeeks(weeksToAdd)
    }
    withTheDay.withTime(timeOfDay)
  }

}

object MonthlyByNthDayOfWeek {
  val recurrenceType = "monthly_by_nth_day_of_week"
}

case class Yearly(frequency: Int, monthDay: MonthDay, timeOfDay: LocalTime) extends Recurrence {

  def nextAfter(previous: DateTime): DateTime = {
    previous.plusYears(frequency).withMonthOfYear(monthDay.getMonthOfYear).withDayOfMonth(monthDay.getDayOfMonth).withTime(timeOfDay)
  }

}

object Yearly {
  val recurrenceType = "yearly"
}

object Recurrence {

  private def buildFrom(
                 recurrenceType: String,
                 frequency: Int,
                 maybeTimeOfDay: Option[LocalTime],
                 maybeMinuteOfHour: Option[Int],
                 maybeDayOfWeek: Option[Int],
                 maybeDayOfMonth: Option[Int],
                 maybeNthDayOfWeek: Option[Int],
                 maybeMonth: Option[Int]
                 ): Recurrence = {
    recurrenceType match {
      case(Hourly.recurrenceType) => Hourly(frequency, maybeMinuteOfHour.get)
      case(Daily.recurrenceType) => Daily(frequency, maybeTimeOfDay.get)
      case(Weekly.recurrenceType) => Weekly(frequency, maybeDayOfWeek.get, maybeTimeOfDay.get)
      case(MonthlyByDayOfMonth.recurrenceType) => MonthlyByDayOfMonth(frequency, maybeDayOfMonth.get, maybeTimeOfDay.get)
      case(MonthlyByNthDayOfWeek.recurrenceType) => MonthlyByNthDayOfWeek(frequency, maybeDayOfWeek.get, maybeNthDayOfWeek.get, maybeTimeOfDay.get)
      case(Yearly.recurrenceType) => Yearly(frequency, new MonthDay(maybeMonth.get, maybeDayOfMonth.get), maybeTimeOfDay.get)
    }
  }

  def buildFor(raw: RawScheduleTrigger): Recurrence = {
    buildFrom(
      raw.recurrenceType,
      raw.frequency,
      raw.maybeTimeOfDay,
      raw.maybeMinuteOfHour,
      raw.maybeDayOfWeek,
      raw.maybeDayOfMonth,
      raw.maybeNthDayOfWeek,
      raw.maybeMonth
    )
  }
}
