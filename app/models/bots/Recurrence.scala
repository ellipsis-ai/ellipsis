package models.bots

import java.util.Date
import org.joda.time.{DateTime, LocalTime, MonthDay}
import com.joestelmach.natty._

sealed trait Recurrence {
  val frequency: Int
  val typeName: String
  val maybeTimeOfDay: Option[LocalTime] = None
  val maybeMinuteOfHour: Option[Int] = None
  val maybeDayOfWeek: Option[Int] = None
  val maybeDayOfMonth: Option[Int] = None
  val maybeNthDayOfWeek: Option[Int] = None
  val maybeMonth: Option[Int] = None
  def nextAfter(previous: DateTime): DateTime
  def initialAfter(start: DateTime): DateTime
  def withStandardAdjustments(when: DateTime): DateTime = when.withSecondOfMinute(0).withMillisOfSecond(0)
}
case class Hourly(frequency: Int, minuteOfHour: Int) extends Recurrence {

  def isEarlierInHour(when: DateTime): Boolean = when.getMinuteOfHour < minuteOfHour
  def isLaterInHour(when: DateTime): Boolean = when.getMinuteOfHour > minuteOfHour

  def withAdjustments(when: DateTime): DateTime = withStandardAdjustments(when.withMinuteOfHour(minuteOfHour))

  def nextAfter(previous: DateTime): DateTime = {
    val hoursToAdd = if (isEarlierInHour(previous)) {
      frequency - 1
    } else {
      frequency
    }
    withAdjustments(previous.plusHours(hoursToAdd))
  }

  def initialAfter(start: DateTime): DateTime = {
    if (isLaterInHour(start)) {
      withAdjustments(start.plusHours(1))
    } else {
      withAdjustments(start)
    }
  }

  val typeName = Hourly.recurrenceType
  override val maybeMinuteOfHour = Some(minuteOfHour)
}

object Hourly {
  val recurrenceType = "hourly"

  def maybeFromText(text: String): Option[Hourly] = {
    val singleRegex = """(?i)every hour.*""".r
    val nRegex = """(?i)every\s+(\d+)\s+hours?.*""".r
    val maybeFrequency = text match {
      case singleRegex() => Some(1)
      case nRegex(frequency) => Some(frequency.toInt)
      case _ => None
    }
    maybeFrequency.map { frequency =>
      val minutesRegex = """.*at\s+(\d+)\s+minutes?.*""".r
      val maybeMinuteOfHour = text match {
        case minutesRegex(minutes) => Some(minutes.toInt)
        case _ => None
      }
      Hourly(frequency, maybeMinuteOfHour.getOrElse(DateTime.now.getMinuteOfHour))
    }
  }
}

case class Daily(frequency: Int, timeOfDay: LocalTime) extends Recurrence {

  def isEarlierInDay(when: DateTime): Boolean = when.toLocalTime.isBefore(timeOfDay)
  def isLaterInDay(when: DateTime): Boolean = when.toLocalTime.isAfter(timeOfDay)

  def withAdjustments(when: DateTime): DateTime = withStandardAdjustments(when.withTime(timeOfDay))

  def nextAfter(previous: DateTime): DateTime = {
    val daysToAdd = if (isEarlierInDay(previous)) {
      frequency - 1
    } else {
      frequency
    }
    withAdjustments(previous.plusDays(daysToAdd))
  }

  def initialAfter(start: DateTime): DateTime = {
    if (isLaterInDay(start)) {
      withAdjustments(start.plusDays(1))
    } else {
      withAdjustments(start)
    }
  }

  val typeName = Daily.recurrenceType
  override val maybeTimeOfDay = Some(timeOfDay)

}

object Daily {
  val recurrenceType = "daily"

  def maybeFromText(text: String): Option[Daily] = {
    val singleRegex = """(?i)every day.*""".r
    val nRegex = """(?i)every\s+(\d+)\s+days?.*""".r
    val maybeFrequency = text match {
      case singleRegex() => Some(1)
      case nRegex(frequency) => Some(frequency.toInt)
      case _ => None
    }
    maybeFrequency.map { frequency =>
      val timeRegex = """(?i).*at\s+(.*)""".r
      val maybeTime = text match {
        case timeRegex(time) => Recurrence.maybeTimeFrom(time)
        case _ => None
      }
      Daily(frequency, maybeTime.getOrElse(Recurrence.currentAdjustedTime))
    }
  }
}

case class Weekly(frequency: Int, dayOfWeek: Int, timeOfDay: LocalTime) extends Recurrence {

  def isEarlierInWeek(when: DateTime): Boolean = {
    when.getDayOfWeek < dayOfWeek || (when.getDayOfWeek == dayOfWeek && when.toLocalTime.isBefore(timeOfDay))
  }
  def isLaterInWeek(when: DateTime): Boolean = {
    when.getDayOfWeek > dayOfWeek || (when.getDayOfWeek == dayOfWeek && when.toLocalTime.isAfter(timeOfDay))
  }

  def withAdjustments(when: DateTime): DateTime = withStandardAdjustments(when.withDayOfWeek(dayOfWeek).withTime(timeOfDay))

  def nextAfter(previous: DateTime): DateTime = {
    val weeksToAdd = if (isEarlierInWeek(previous)) {
      frequency - 1
    } else {
      frequency
    }
    withAdjustments(previous.plusWeeks(weeksToAdd))
  }

  def initialAfter(start: DateTime): DateTime = {
    if (isLaterInWeek(start)) {
      withAdjustments(start.plusWeeks(1))
    } else {
      withAdjustments(start)
    }
  }

  val typeName = Weekly.recurrenceType
  override val maybeDayOfWeek = Some(dayOfWeek)
  override val maybeTimeOfDay = Some(timeOfDay)

}

object Weekly {
  val recurrenceType = "weekly"

  def maybeFromText(text: String): Option[Weekly] = {
    val singleRegex = """(?i)every week.*""".r
    val nRegex = """(?i)every\s+(\d+)\s+weeks?.*""".r
    val maybeFrequency = text match {
      case singleRegex() => Some(1)
      case nRegex(frequency) => Some(frequency.toInt)
      case _ => None
    }
    maybeFrequency.map { frequency =>
      val dayRegex = """(?i).*on\s+(\S+).*""".r
      val maybeDayOfWeek = text match {
        case dayRegex(day) => Recurrence.maybeDayOfWeekFrom(day)
        case _ => None
      }
      val timeRegex = """(?i).*at\s+(.*)""".r
      val maybeTime = text match {
        case timeRegex(time) => Recurrence.maybeTimeFrom(time)
        case _ => None
      }
      Weekly(frequency, maybeDayOfWeek.getOrElse(DateTime.now.getDayOfWeek), maybeTime.getOrElse(Recurrence.currentAdjustedTime))
    }
  }
}

case class MonthlyByDayOfMonth(frequency: Int, dayOfMonth: Int, timeOfDay: LocalTime) extends Recurrence {

  def isEarlierInMonth(when: DateTime): Boolean = {
    when.getDayOfMonth < dayOfMonth || (when.getDayOfMonth == dayOfMonth && when.toLocalTime.isBefore(timeOfDay))
  }
  def isLaterInMonth(when: DateTime): Boolean = {
    when.getDayOfMonth > dayOfMonth || (when.getDayOfMonth == dayOfMonth && when.toLocalTime.isAfter(timeOfDay))
  }

  def withAdjustments(when: DateTime): DateTime = withStandardAdjustments(when.withDayOfMonth(dayOfMonth).withTime(timeOfDay))

  def nextAfter(previous: DateTime): DateTime = {
    val monthsToAdd = if (isEarlierInMonth(previous)) {
      frequency - 1
    } else {
      frequency
    }
    withAdjustments(previous.plusMonths(monthsToAdd))
  }

  def initialAfter(start: DateTime): DateTime = {
    if (isLaterInMonth(start)) {
      withAdjustments(start.plusMonths(1))
    } else {
      withAdjustments(start)
    }
  }

  val typeName = MonthlyByDayOfMonth.recurrenceType
  override val maybeDayOfMonth = Some(dayOfMonth)
  override val maybeTimeOfDay = Some(timeOfDay)

}

object MonthlyByDayOfMonth {
  val recurrenceType = "monthly_by_day_of_month"
}

case class MonthlyByNthDayOfWeek(frequency: Int, dayOfWeek: Int, nth: Int, timeOfDay: LocalTime) extends Recurrence {

  def targetInMonthMatching(when: DateTime): DateTime = {
    val firstOfTheMonth = when.withDayOfMonth(1)
    val weeksToAdd = if (firstOfTheMonth.getDayOfWeek <= dayOfWeek) {
      nth - 1
    } else {
      nth
    }
    withStandardAdjustments(firstOfTheMonth.plusWeeks(weeksToAdd).withDayOfWeek(dayOfWeek).withTime(timeOfDay))
  }

  def nextAfter(previous: DateTime): DateTime = {
    val monthsToAdd = if (targetInMonthMatching(previous).isAfter(previous)) {
      frequency - 1
    } else {
      frequency
    }
    targetInMonthMatching(previous.plusMonths(monthsToAdd))
  }

  def initialAfter(start: DateTime): DateTime = {
    if (targetInMonthMatching(start).isBefore(start)) {
      targetInMonthMatching(start.plusMonths(1))
    } else {
      targetInMonthMatching(start)
    }
  }

  val typeName = MonthlyByNthDayOfWeek.recurrenceType
  override val maybeDayOfWeek = Some(dayOfWeek)
  override val maybeNthDayOfWeek = Some(nth)
  override val maybeTimeOfDay = Some(timeOfDay)

}

object MonthlyByNthDayOfWeek {
  val recurrenceType = "monthly_by_nth_day_of_week"
}

case class Yearly(frequency: Int, monthDay: MonthDay, timeOfDay: LocalTime) extends Recurrence {

  val month = monthDay.getMonthOfYear
  val dayOfMonth = monthDay.getDayOfMonth

  def isEarlierInYear(when: DateTime): Boolean = {
    when.getMonthOfYear < month ||
      (when.getMonthOfYear == month && when.getDayOfMonth < dayOfMonth) ||
      (when.getMonthOfYear == month && when.getDayOfMonth == dayOfMonth && when.toLocalTime.isBefore(timeOfDay))
  }
  def isLaterInYear(when: DateTime): Boolean = {
    when.getMonthOfYear > month ||
      (when.getMonthOfYear == month && when.getDayOfMonth > dayOfMonth) ||
      (when.getMonthOfYear == month && when.getDayOfMonth == dayOfMonth && when.toLocalTime.isAfter(timeOfDay))
  }

  def withAdjustments(when: DateTime): DateTime = withStandardAdjustments(when.withMonthOfYear(month).withDayOfMonth(dayOfMonth).withTime(timeOfDay))

  def nextAfter(previous: DateTime): DateTime = {
    val yearsToAdd = if (isEarlierInYear(previous)) {
      frequency - 1
    } else {
      frequency
    }
    withAdjustments(previous.plusYears(yearsToAdd))
  }

  def initialAfter(start: DateTime): DateTime = {
    if (isLaterInYear(start)) {
      withAdjustments(start.plusYears(1))
    } else {
      withAdjustments(start)
    }
  }

  val typeName = Yearly.recurrenceType
  override val maybeMonth = Some(month)
  override val maybeDayOfMonth = Some(dayOfMonth)
  override val maybeTimeOfDay = Some(timeOfDay)

}

object Yearly {
  val recurrenceType = "yearly"
}

object Recurrence {

  def currentAdjustedTime: LocalTime = DateTime.now.toLocalTime.withSecondOfMinute(0).withMillisOfSecond(0)

  private def maybeDateFrom(text: String): Option[Date] = {
    val parser = new Parser()
    val groups = parser.parse(text)
    if (groups.isEmpty || groups.get(0).getDates.isEmpty) {
      None
    } else {
      Some(groups.get(0).getDates.get(0))
    }
  }

  def maybeTimeFrom(text: String): Option[LocalTime] = {
    maybeDateFrom(text).map { date =>
      new LocalTime(date.getTime)
    }
  }

  def maybeDayOfWeekFrom(text: String): Option[Int] = {
    maybeDateFrom(text).map { date =>
      new DateTime(date).getDayOfWeek
    }
  }

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

  def buildFor(raw: RawScheduledMessage): Recurrence = {
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

  def maybeFromText(text: String): Option[Recurrence] = {
    Hourly.maybeFromText(text).orElse {
      Daily.maybeFromText(text).orElse {
        Weekly.maybeFromText(text).orElse {
          None
        }
      }
    }
  }
}
