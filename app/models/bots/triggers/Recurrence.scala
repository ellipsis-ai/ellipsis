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

case class Daily(frequency: Int, timeOfDay: LocalTime) extends Recurrence {

  def nextAfter(previous: DateTime): DateTime = {
    previous.plusDays(frequency).withTime(timeOfDay)
  }

}

case class Weekly(frequency: Int, dayOfWeek: Int, timeOfDay: LocalTime) extends Recurrence {

  def nextAfter(previous: DateTime): DateTime = {
    previous.plusWeeks(frequency).withDayOfWeek(dayOfWeek).withTime(timeOfDay)
  }

}

case class MonthlyByDayOfMonth(frequency: Int, dayOfMonth: Int, timeOfDay: LocalTime) extends Recurrence {

  def nextAfter(previous: DateTime): DateTime = {
    previous.plusMonths(frequency).withDayOfMonth(dayOfMonth).withTime(timeOfDay)
  }

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

case class Yearly(frequency: Int, monthDay: MonthDay, timeOfDay: LocalTime) extends Recurrence {

  def nextAfter(previous: DateTime): DateTime = {
    previous.plusYears(frequency).withMonthOfYear(monthDay.getMonthOfYear).withDayOfMonth(monthDay.getDayOfMonth).withTime(timeOfDay)
  }

}
