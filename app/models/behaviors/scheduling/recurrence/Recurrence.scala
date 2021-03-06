package models.behaviors.scheduling.recurrence

import java.time._
import java.time.format.{DateTimeFormatter, TextStyle}
import java.time.temporal.TemporalAdjusters
import java.util.{Calendar, Date, Locale, TimeZone}

import com.joestelmach.natty._
import models.IDs

import scala.collection.mutable.ArrayBuffer
import scala.util.matching.Regex

sealed trait Recurrence {
  val id: String

  // for testing
  def copyWithEmptyId: Recurrence

  val frequency: Int
  val typeName: String

  val timesHasRun: Int
  val maybeTotalTimesToRun: Option[Int]

  def shouldRunAgainAfterNextRun: Boolean = {
    maybeTotalTimesToRun.isEmpty || maybeTotalTimesToRun.exists { totalTimesToRun =>
      timesHasRun + 1 < totalTimesToRun
    }
  }

  def incrementTimesHasRun: Recurrence

  val maybeTimeOfDay: Option[LocalTime] = None
  val maybeTimeZone: Option[ZoneId] = None
  def maybeZoneOffsetAt(when: OffsetDateTime): Option[ZoneOffset] = {
    maybeTimeZone.map { timeZone =>
      Recurrence.zoneOffsetAt(when, timeZone)
    }
  }
  val maybeMinuteOfHour: Option[Int] = None
  val daysOfWeek: Seq[DayOfWeek] = Seq()
  val maybeMonday: Option[Boolean] = Some(daysOfWeek.contains(DayOfWeek.MONDAY))
  val maybeTuesday: Option[Boolean] = Some(daysOfWeek.contains(DayOfWeek.TUESDAY))
  val maybeWednesday: Option[Boolean] = Some(daysOfWeek.contains(DayOfWeek.WEDNESDAY))
  val maybeThursday: Option[Boolean] = Some(daysOfWeek.contains(DayOfWeek.THURSDAY))
  val maybeFriday: Option[Boolean] = Some(daysOfWeek.contains(DayOfWeek.FRIDAY))
  val maybeSaturday: Option[Boolean] = Some(daysOfWeek.contains(DayOfWeek.SATURDAY))
  val maybeSunday: Option[Boolean] = Some(daysOfWeek.contains(DayOfWeek.SUNDAY))
  val maybeDayOfWeek: Option[DayOfWeek] = None
  val maybeDayOfMonth: Option[Int] = None
  val maybeNthDayOfWeek: Option[Int] = None
  val maybeMonth: Option[Int] = None
  protected def withZone(when: OffsetDateTime): OffsetDateTime = {
    maybeZoneOffsetAt(when).map { offset =>
      when.withOffsetSameInstant(offset)
    }.getOrElse(when)
  }
  protected def nextAfterAssumingZone(previous: OffsetDateTime): OffsetDateTime
  def nextAfter(previous: OffsetDateTime): OffsetDateTime = {
    nextAfterAssumingZone(withZone(previous))
  }
  protected def initialAfterAssumingZone(start: OffsetDateTime): OffsetDateTime
  def initialAfter(start: OffsetDateTime): OffsetDateTime = {
    initialAfterAssumingZone(withZone(start))
  }
  def withStandardAdjustments(when: OffsetDateTime): OffsetDateTime = when.withSecond(0).withNano(0)
  def displayString: String
  def couldRunAt(when: OffsetDateTime): Boolean
  def expectedNextRunFor(start: OffsetDateTime, maybeProposedNextRun: Option[OffsetDateTime]): OffsetDateTime = {
    val expectedFirstRun = initialAfter(start)
    val expectedSecondRun = nextAfter(expectedFirstRun)
    // Keep the proposed next run as long as it would happen before the second possible run after now
    maybeProposedNextRun.filter { proposedNextRun =>
      couldRunAt(proposedNextRun) && proposedNextRun.isAfter(start) && proposedNextRun.isBefore(expectedSecondRun)
    }.getOrElse(expectedFirstRun)
  }


  def timesToRunString: String = {
    maybeTotalTimesToRun.map { timesToRun =>
      val timesRemaining = timesToRun - timesHasRun
      if (timesToRun == 1) {
        ", once"
      } else if (timesToRun > 1 && timesRemaining == 1) {
        s", for the last time (out of ${timesToRun} total)"
      } else if (timesRemaining > 1) {
        if (timesRemaining < timesToRun) {
          s", ${timesRemaining} more times (out of ${timesToRun} total)"
        } else {
          s", ${timesToRun} times"
        }
      } else {
        ""
      }
    }.getOrElse("")
  }

  def toRaw: RawRecurrence = RawRecurrence(
    id,
    typeName,
    frequency: Int,
    timesHasRun,
    maybeTotalTimesToRun,
    maybeTimeOfDay,
    maybeTimeZone,
    maybeMinuteOfHour,
    maybeDayOfWeek.map(_.getValue),
    maybeMonday,
    maybeTuesday,
    maybeWednesday,
    maybeThursday,
    maybeFriday,
    maybeSaturday,
    maybeSunday,
    maybeDayOfMonth,
    maybeNthDayOfWeek,
    maybeMonth
  )
}

case class Minutely(id: String, frequency: Int, timesHasRun: Int, maybeTotalTimesToRun: Option[Int]) extends Recurrence {

  def copyWithEmptyId: Minutely = copy(id = "")

  def incrementTimesHasRun: Minutely = copy(timesHasRun = timesHasRun + 1)

  override def displayString: String = {
    if (maybeTotalTimesToRun.contains(1)) {
      val frequencyString = if (frequency == 1) { "1 minute" } else { s"$frequency minutes" }
      s"in $frequencyString, once"
    } else {
      val frequencyString = if (frequency == 1) { "minute" } else { s"$frequency minutes" }
      s"every $frequencyString$timesToRunString"
    }
  }

  val typeName = Minutely.recurrenceType

  protected def nextAfterAssumingZone(previous: OffsetDateTime): OffsetDateTime = {
    withStandardAdjustments(previous.plusMinutes(frequency))
  }

  protected def initialAfterAssumingZone(start: OffsetDateTime): OffsetDateTime = {
    withStandardAdjustments(start)
  }

  def couldRunAt(when: OffsetDateTime): Boolean = true
}

object Minutely {
  val recurrenceType = "minutely"

  def maybeUnsavedFromText(text: String): Option[Minutely] = {
    val singleRegex = """(?i).*every minute.*""".r
    val nRegex = """(?i).*every\s+(\d+)\s*minutes?.*""".r
    val maybeNTimesRegex = """(?i).*in\s+(\d+)\s*minutes?.*""".r
    val maybeFrequency = text match {
      case singleRegex() => Some(1)
      case nRegex(frequency) => Some(frequency.toInt)
      case _ => None
    }
    val maybeRunNTimes = text match {
      case maybeNTimesRegex(frequency) => Some(frequency.toInt)
      case _ => None
    }
    maybeFrequency.orElse(maybeRunNTimes).map { frequency =>
      val maybeTimesToRun = Recurrence.maybeTimesToRunFromText(text).orElse {
        if (maybeRunNTimes.isDefined) {
          Some(1)
        } else {
          None
        }
      }
      Minutely(IDs.next, frequency, 0, maybeTimesToRun)
    }
  }
}

trait RecurrenceWithTimeZone extends Recurrence {
  val timeZone: ZoneId
  override val maybeTimeZone = Some(timeZone)

  // TODO: Someday we may care about locales
  def stringFor(timeZone: ZoneId): String = s"${timeZone.getDisplayName(TextStyle.FULL, Locale.ENGLISH)}"
}

case class Hourly(id: String, frequency: Int, timesHasRun: Int, maybeTotalTimesToRun: Option[Int], minuteOfHour: Int, timeZone: ZoneId) extends RecurrenceWithTimeZone {

  def copyWithEmptyId: Hourly = copy(id = "")

  def incrementTimesHasRun: Hourly = copy(timesHasRun = timesHasRun + 1)

  def displayString: String = {
    if (maybeTotalTimesToRun.contains(1)) {
      val frequencyString = if (frequency == 1) { "the next hour" } else { s"$frequency hours" }
      s"in $frequencyString at $minuteOfHour (${stringFor(timeZone)}), once"
    } else {
      val frequencyString = if (frequency == 1) {"hour"} else {s"$frequency hours"}
      s"every $frequencyString at $minuteOfHour minutes past (${stringFor(timeZone)})$timesToRunString"
    }
  }

  def isEarlierInHour(when: OffsetDateTime): Boolean = {
    withZone(when).getMinute < minuteOfHour
  }

  def isLaterInHour(when: OffsetDateTime): Boolean = {
    withZone(when).getMinute > minuteOfHour
  }

  def withAdjustments(when: OffsetDateTime): OffsetDateTime = {
    withStandardAdjustments(withZone(when).withMinute(minuteOfHour))
  }

  protected def nextAfterAssumingZone(previous: OffsetDateTime): OffsetDateTime = {
    val hoursToAdd = if (isEarlierInHour(previous)) {
      frequency - 1
    } else {
      frequency
    }
    withAdjustments(previous.plusHours(hoursToAdd))
  }

  protected def initialAfterAssumingZone(start: OffsetDateTime): OffsetDateTime = {
    if (isLaterInHour(start)) {
      withAdjustments(start.plusHours(1))
    } else {
      withAdjustments(start)
    }
  }

  def couldRunAt(when: OffsetDateTime): Boolean = {
    val inSameTimeZone = when.atZoneSameInstant(timeZone).toOffsetDateTime
    inSameTimeZone.getMinute == minuteOfHour
  }

  val typeName = Hourly.recurrenceType
  override val maybeMinuteOfHour = Some(minuteOfHour)
}

object Hourly {
  val recurrenceType = "hourly"

  def maybeUnsavedFromText(text: String, defaultTimeZone: ZoneId): Option[Hourly] = {
    val singleRegex = """(?i).*every hour.*""".r
    val nRegex = """(?i).*every\s+(\d+)\s+hours?.*""".r
    val maybeNTimesRegex = """(?i).*in\s+(\d+)\s*hours?.*""".r
    val maybeFrequency = text match {
      case singleRegex() => Some(1)
      case nRegex(frequency) => Some(frequency.toInt)
      case _ => None
    }
    val maybeRunNTimes = text match {
      case maybeNTimesRegex(frequency) => Some(frequency.toInt)
      case _ => None
    }
    maybeFrequency.orElse(maybeRunNTimes).map { frequency =>
      val minutesRegex = """.*at\s+(\d+)\s+minutes?.*""".r
      val maybeMinuteOfHour = text match {
        case minutesRegex(minutes) => Some(minutes.toInt)
        case _ => None
      }
      val maybeTimesToRun = Recurrence.maybeTimesToRunFromText(text).orElse {
        if (maybeRunNTimes.isDefined) {
          Some(1)
        } else {
          None
        }
      }
      Hourly(IDs.next, frequency, 0, maybeTimesToRun, maybeMinuteOfHour.getOrElse(OffsetDateTime.now.getMinute), defaultTimeZone)
    }
  }
}

trait RecurrenceWithTimeOfDay extends RecurrenceWithTimeZone {
  val timeOfDay: LocalTime
  val hourOfDay = timeOfDay.getHour
  val minuteOfHour = timeOfDay.getMinute
  val secondOfMinute = timeOfDay.getSecond
  val nanosOfSecond = timeOfDay.getNano

  def timeOfDayFormatted = s"${timeOfDay.format(Recurrence.timeFormatter)} ${stringFor(timeZone)}"

  override def withStandardAdjustments(when: OffsetDateTime): OffsetDateTime = {
    super.withStandardAdjustments(withTime(when))
  }

  def withTime(when: OffsetDateTime): OffsetDateTime = {
    withZone(when).withHour(hourOfDay).withMinute(minuteOfHour).withSecond(secondOfMinute).withNano(nanosOfSecond)
  }
}

case class Daily(id: String, frequency: Int, timesHasRun: Int, maybeTotalTimesToRun: Option[Int], timeOfDay: LocalTime, timeZone: ZoneId) extends RecurrenceWithTimeOfDay {

  def copyWithEmptyId: Daily = copy(id = "")

  def incrementTimesHasRun: Daily = copy(timesHasRun = timesHasRun + 1)

  def displayString: String = {
    if (maybeTotalTimesToRun.contains(1)) {
      val frequencyString = if (frequency == 1) {"at"} else {s"in $frequency days, at"}
      s"$frequencyString $timeOfDayFormatted, once"
    } else {
      val frequencyString = if (frequency == 1) {"day"} else {s"$frequency days"}
      s"every $frequencyString at $timeOfDayFormatted$timesToRunString"
    }
  }

  def isEarlierInDay(when: OffsetDateTime): Boolean = when.toLocalTime.isBefore(timeOfDay)
  def isLaterInDay(when: OffsetDateTime): Boolean = when.toLocalTime.isAfter(timeOfDay)

  def withAdjustments(when: OffsetDateTime): OffsetDateTime = withStandardAdjustments(when)

  protected def nextAfterAssumingZone(previous: OffsetDateTime): OffsetDateTime = {
    val daysToAdd = if (isEarlierInDay(previous)) {
      frequency - 1
    } else {
      frequency
    }
    withAdjustments(previous.plusDays(daysToAdd))
  }

  protected def initialAfterAssumingZone(start: OffsetDateTime): OffsetDateTime = {
    if (isLaterInDay(start)) {
      withAdjustments(start.plusDays(1))
    } else {
      withAdjustments(start)
    }
  }

  val typeName = Daily.recurrenceType
  override val maybeTimeOfDay = Some(timeOfDay)

  def couldRunAt(when: OffsetDateTime): Boolean = {
    val inSameTimeZone = when.atZoneSameInstant(timeZone).toOffsetDateTime
    inSameTimeZone.getMinute == timeOfDay.getMinute && inSameTimeZone.getHour == timeOfDay.getHour
  }
}

object Daily {
  val recurrenceType = "daily"

  val TODAY = "today"
  val TOMORROW = "tomorrow"

  def maybeNextInstanceForTodayOrTomorrow(todayOrTomorrow: String, desiredTime: LocalTime, currentTime: LocalTime): Option[Int] = {
    val isLaterThanNow = desiredTime.isAfter(currentTime) || desiredTime.equals(currentTime)
    if (isLaterThanNow && todayOrTomorrow == TOMORROW) {
      Some(2)
    } else if (!isLaterThanNow && todayOrTomorrow == TOMORROW || isLaterThanNow && todayOrTomorrow == TODAY) {
      Some(1)
    } else {
      None
    }
  }

  def maybeUnsavedFromText(text: String, defaultTimeZone: ZoneId): Option[Daily] = {
    val singleRegex = """(?i)every day.*""".r
    val nRegex = """(?i)every\s+(\d+)\s+days?.*""".r
    val todayRegex = """(?i)today\s+at.*""".r
    val tomorrowRegex = """(?i)tomorrow\s+at.*""".r
    val maybeFrequency = text match {
      case singleRegex() => Some(1)
      case nRegex(frequency) => Some(frequency.toInt)
      case _ => None
    }
    val maybeTodayOrTomorrow = text match {
      case todayRegex() => Some(TODAY)
      case tomorrowRegex() => Some(TOMORROW)
      case _ => None
    }
    val maybeTime = Recurrence.maybeTimeFrom(text, defaultTimeZone)
    val maybeTimesToRun = Recurrence.maybeTimesToRunFromText(text)
    maybeFrequency.map { frequency =>
      Daily(IDs.next, frequency, 0, maybeTimesToRun, maybeTime.getOrElse(Recurrence.currentAdjustedTime(defaultTimeZone)), defaultTimeZone)
    }.orElse {
      for {
        todayOrTomorrow <- maybeTodayOrTomorrow
        desiredTime <- maybeTime
        nextInstance <- maybeNextInstanceForTodayOrTomorrow(todayOrTomorrow, desiredTime, LocalTime.now(defaultTimeZone))
      } yield {
        Daily(IDs.next, nextInstance, 0, Some(1), desiredTime, defaultTimeZone)
      }
    }
  }
}

case class Weekly(
                   id: String,
                   frequency: Int,
                   timesHasRun: Int,
                   maybeTotalTimesToRun: Option[Int],
                   override val daysOfWeek: Seq[DayOfWeek],
                   timeOfDay: LocalTime,
                   timeZone: ZoneId
                 ) extends RecurrenceWithTimeOfDay {

  def copyWithEmptyId: Weekly = copy(id = "")

  def incrementTimesHasRun: Weekly = copy(timesHasRun = timesHasRun + 1)

  lazy val daysOfWeekValues = daysOfWeek.map(_.getValue)

  lazy val daysOfWeekString = {
    if (daysOfWeek.length == 5 && !daysOfWeek.contains(DayOfWeek.SATURDAY) && !daysOfWeek.contains(DayOfWeek.SUNDAY)) {
      "weekday"
    } else if (daysOfWeek.length == 7) {
      "day"
    } else {
      daysOfWeek.map(Recurrence.dayOfWeekNameFor).mkString(", ")
    }
  }

  def maybeNextDayInWeekOf(when: OffsetDateTime): Option[DayOfWeek] = {
    if (isEarlierTheSameDay(when)) {
      Some(when.getDayOfWeek)
    } else {
      daysOfWeek.find(ea => ea.getValue > when.getDayOfWeek.getValue)
    }
  }

  def nextDayOfWeekFor(when: OffsetDateTime): DayOfWeek = {
    maybeNextDayInWeekOf(when).getOrElse(daysOfWeek.head)
  }

  def displayString: String = {
    if (maybeTotalTimesToRun.contains(1)) {
      val frequencyString = if (frequency == 1) { s"next $daysOfWeekString" } else { s"in $frequency weeks on each $daysOfWeekString"}
      s"$frequencyString at $timeOfDayFormatted, once"
    } else {
      val frequencyString = if (frequency == 1) {daysOfWeekString} else {s"$frequency weeks on $daysOfWeekString"}
      s"every $frequencyString at $timeOfDayFormatted$timesToRunString"
    }
  }

  def isEarlierTheSameDay(when: OffsetDateTime): Boolean = {
    daysOfWeek.contains(when.getDayOfWeek) && when.toLocalTime.isBefore(timeOfDay)
  }

  def isEarlierInWeek(when: OffsetDateTime): Boolean = {
    maybeNextDayInWeekOf(when).isDefined
  }
  def isLaterInWeek(when: OffsetDateTime): Boolean = !isEarlierInWeek(when)

  def withAdjustments(when: OffsetDateTime): OffsetDateTime = {
    withStandardAdjustments(when.`with`(nextDayOfWeekFor(when)))
  }

  protected def nextAfterAssumingZone(previous: OffsetDateTime): OffsetDateTime = {
    val weeksToAdd = if (isEarlierInWeek(previous)) {
      frequency - 1
    } else {
      frequency
    }
    withAdjustments(previous.plusWeeks(weeksToAdd))
  }

  protected def initialAfterAssumingZone(start: OffsetDateTime): OffsetDateTime = {
    if (isLaterInWeek(start)) {
      withAdjustments(start.plusWeeks(1))
    } else {
      withAdjustments(start)
    }
  }

  def couldRunAt(when: OffsetDateTime): Boolean = {
    val inSameTimeZone = when.atZoneSameInstant(timeZone).toOffsetDateTime
    inSameTimeZone.getMinute == timeOfDay.getMinute && inSameTimeZone.getHour == timeOfDay.getHour && daysOfWeek.contains(inSameTimeZone.getDayOfWeek)
  }

  val typeName = Weekly.recurrenceType
  override val maybeTimeOfDay = Some(timeOfDay)

}

object Weekly {
  val recurrenceType = "weekly"

  def maybeUnsavedFromText(text: String, defaultTimeZone: ZoneId): Option[Weekly] = {
    val singleRegex = """(?i)every week.*""".r
    val nRegex = """(?i)every\s+(\d+)\s+weeks?.*""".r
    val maybeFrequency = text match {
      case singleRegex() => Some(1)
      case nRegex(frequency) => Some(frequency.toInt)
      case _ => None
    }
    var daysOfWeek = Recurrence.daysOfWeekFrom(text)
    val maybeTime = Recurrence.maybeTimeFrom(text, defaultTimeZone)
    val maybeTimesToRun = Recurrence.maybeTimesToRunFromText(text)
    maybeFrequency.map { frequency =>
      if (daysOfWeek.isEmpty) {
        daysOfWeek = Seq(OffsetDateTime.now.getDayOfWeek)
      }
      Weekly(IDs.next, frequency, 0, maybeTimesToRun, daysOfWeek, maybeTime.getOrElse(Recurrence.currentAdjustedTime(defaultTimeZone)), defaultTimeZone)
    }.orElse {
      if (daysOfWeek.length > 0 && Recurrence.maybeMonthlyFrequencyFrom(text).isEmpty) {
        maybeTime.map { timeOfDay =>
          val maybeTimesToRun = if (text.toLowerCase.contains("every")) {
            None
          } else {
            Some(daysOfWeek.length)
          }
          Weekly(IDs.next, 1, 0, maybeTimesToRun, daysOfWeek, timeOfDay, defaultTimeZone)
        }
      } else {
        None
      }
    }
  }
}

case class MonthlyByDayOfMonth(id: String, frequency: Int, timesHasRun: Int, maybeTotalTimesToRun: Option[Int], dayOfMonth: Int, timeOfDay: LocalTime, timeZone: ZoneId) extends RecurrenceWithTimeOfDay {

  def copyWithEmptyId: MonthlyByDayOfMonth = copy(id = "")

  def incrementTimesHasRun: MonthlyByDayOfMonth = copy(timesHasRun = timesHasRun + 1)

  def displayString: String = {
    val dayOfMonthString = Recurrence.ordinalStringFor(dayOfMonth)
    if (maybeTotalTimesToRun.contains(1)) {
      val frequencyString = if (frequency == 1) { "" } else { s"in $frequency months, "}
      s"${frequencyString}on the $dayOfMonthString of the month at $timeOfDayFormatted, once"
    } else {
      val frequencyString = if (frequency == 1) {"month"} else {s"$frequency months"}
      s"every $frequencyString on the $dayOfMonthString at $timeOfDayFormatted$timesToRunString"
    }
  }

  def isEarlierInMonth(when: OffsetDateTime): Boolean = {
    when.getDayOfMonth < adjustedDayOfMonth(when) || (when.getDayOfMonth == adjustedDayOfMonth(when) && when.toLocalTime.isBefore(timeOfDay))
  }
  def isLaterInMonth(when: OffsetDateTime): Boolean = {
    when.getDayOfMonth > adjustedDayOfMonth(when) || (when.getDayOfMonth == adjustedDayOfMonth(when) && when.toLocalTime.isAfter(timeOfDay))
  }

  private def adjustedDayOfMonth(when: OffsetDateTime): Int = {
    // If the recurrence day of month is after the last possible day of the given month,
    // use the last possible day (e.g. use November 30 if the desired day is 31)
    val lastDayOfMonth = when.`with`(TemporalAdjusters.lastDayOfMonth()).getDayOfMonth
    Math.min(dayOfMonth, lastDayOfMonth)
  }

  private def withAdjustedDayOfMonth(when: OffsetDateTime): OffsetDateTime = {
    when.withDayOfMonth(adjustedDayOfMonth(when))
  }

  def withAdjustments(when: OffsetDateTime): OffsetDateTime = {
    withStandardAdjustments(withAdjustedDayOfMonth(when))
  }

  protected def nextAfterAssumingZone(previous: OffsetDateTime): OffsetDateTime = {
    val monthsToAdd = if (isEarlierInMonth(previous)) {
      frequency - 1
    } else {
      frequency
    }
    withAdjustments(previous.plusMonths(monthsToAdd))
  }

  protected def initialAfterAssumingZone(start: OffsetDateTime): OffsetDateTime = {
    if (isLaterInMonth(start)) {
      withAdjustments(start.plusMonths(1))
    } else {
      withAdjustments(start)
    }
  }

  def couldRunAt(when: OffsetDateTime): Boolean = {
    val inSameTimeZone = when.atZoneSameInstant(timeZone).toOffsetDateTime
    inSameTimeZone.getMinute == timeOfDay.getMinute && inSameTimeZone.getHour == timeOfDay.getHour && inSameTimeZone.getDayOfMonth == dayOfMonth
  }

  val typeName = MonthlyByDayOfMonth.recurrenceType
  override val maybeDayOfMonth = Some(dayOfMonth)
  override val maybeTimeOfDay = Some(timeOfDay)

}

object MonthlyByDayOfMonth {
  val recurrenceType = "monthly_by_day_of_month"

  private def maybeDayOfMonthFrom(text: String): Option[Int] = {
    Recurrence.maybeOrdinalFor(text, Some(" month"))
  }

  def maybeUnsavedFromText(text: String, defaultTimeZone: ZoneId): Option[MonthlyByDayOfMonth] = {
    if (Recurrence.includesDayOfWeek(text)) {
      return None
    }

    Recurrence.maybeMonthlyFrequencyFrom(text).map { frequency =>
      val maybeDayOfMonth = maybeDayOfMonthFrom(text)
      MonthlyByDayOfMonth(
        IDs.next,
        frequency,
        0,
        Recurrence.maybeTimesToRunFromText(text),
        maybeDayOfMonth.getOrElse(OffsetDateTime.now.getDayOfMonth),
        Recurrence.ensureTimeFrom(text, defaultTimeZone),
        defaultTimeZone
      )
    }
  }
}

case class MonthlyByNthDayOfWeek(id: String, frequency: Int, timesHasRun: Int, maybeTotalTimesToRun: Option[Int], dayOfWeek: DayOfWeek, nth: Int, timeOfDay: LocalTime, timeZone: ZoneId) extends RecurrenceWithTimeOfDay {

  def copyWithEmptyId: MonthlyByNthDayOfWeek = copy(id = "")

  def incrementTimesHasRun: MonthlyByNthDayOfWeek = copy(timesHasRun = timesHasRun + 1)

  def displayString: String = {
    val formattedDayOfWeek = s"${Recurrence.ordinalStringFor(nth)} ${Recurrence.dayOfWeekNameFor(dayOfWeek)}"
    if (maybeTotalTimesToRun.contains(1)) {
      val frequencyString = if (frequency == 1) {""} else {s"in $frequency months, "}
      s"${frequencyString}on the $formattedDayOfWeek of the month at $timeOfDayFormatted, once"
    } else {
      val frequencyString = if (frequency == 1) {"month"} else {s"$frequency months"}
      s"every $frequencyString on the $formattedDayOfWeek at $timeOfDayFormatted$timesToRunString"
    }
  }

  def targetInMonthMatching(when: OffsetDateTime): OffsetDateTime = {
    val firstOfTheMonth = when.withDayOfMonth(1)
    val weeksToAdd = if (firstOfTheMonth.getDayOfWeek.getValue <= dayOfWeek.getValue) {
      nth - 1
    } else {
      nth
    }
    withStandardAdjustments(firstOfTheMonth.plusWeeks(weeksToAdd).`with`(dayOfWeek))
  }

  protected def nextAfterAssumingZone(previous: OffsetDateTime): OffsetDateTime = {
    val monthsToAdd = if (targetInMonthMatching(previous).isAfter(previous)) {
      frequency - 1
    } else {
      frequency
    }
    targetInMonthMatching(previous.plusMonths(monthsToAdd))
  }

  protected def initialAfterAssumingZone(start: OffsetDateTime): OffsetDateTime = {
    if (targetInMonthMatching(start).isBefore(start)) {
      targetInMonthMatching(start.plusMonths(1))
    } else {
      targetInMonthMatching(start)
    }
  }

  def couldRunAt(when: OffsetDateTime): Boolean = {
    val inSameTimeZone = when.atZoneSameInstant(timeZone).toOffsetDateTime
    val target = targetInMonthMatching(inSameTimeZone)
    inSameTimeZone.getMinute == timeOfDay.getMinute && inSameTimeZone.getHour == timeOfDay.getHour && inSameTimeZone.getDayOfMonth == target.getDayOfMonth
  }

  val typeName = MonthlyByNthDayOfWeek.recurrenceType
  override val maybeDayOfWeek = Some(dayOfWeek)
  override val maybeNthDayOfWeek = Some(nth)
  override val maybeTimeOfDay = Some(timeOfDay)

}

case class NthDayOfWeek(dayOfWeek: DayOfWeek, n: Int)

object MonthlyByNthDayOfWeek {
  val recurrenceType = "monthly_by_nth_day_of_week"

  private def maybeNthDayOfWeekFrom(text: String): Option[NthDayOfWeek] = {
    for {
      dayOfWeek <- Recurrence.maybeDayOfWeekFrom(text)
      ordinal <- Recurrence.maybeOrdinalFor(text, Some(" month"))
    } yield NthDayOfWeek(dayOfWeek, ordinal)
  }

  def maybeUnsavedFromText(text: String, defaultTimeZone: ZoneId): Option[MonthlyByNthDayOfWeek] = {
    for {
      nthDayOfWeek <- maybeNthDayOfWeekFrom(text)
      frequency <- Recurrence.maybeMonthlyFrequencyFrom(text)
    } yield {
      MonthlyByNthDayOfWeek(
        IDs.next,
        frequency,
        0,
        Recurrence.maybeTimesToRunFromText(text),
        nthDayOfWeek.dayOfWeek,
        nthDayOfWeek.n,
        Recurrence.ensureTimeFrom(text, defaultTimeZone),
        defaultTimeZone
      )
    }
  }
}

case class Yearly(id: String, frequency: Int, timesHasRun: Int, maybeTotalTimesToRun: Option[Int], monthDay: MonthDay, timeOfDay: LocalTime, timeZone: ZoneId) extends RecurrenceWithTimeOfDay {

  def displayString: String = {
    val formattedMonthDay = monthDay.format(Recurrence.monthDayFormatter)
    if (maybeTotalTimesToRun.contains(1)) {
      val frequencyString = if (frequency == 1) {""} else {s"in $frequency years, "}
      s"${frequencyString}on $formattedMonthDay at $timeOfDayFormatted, once"
    } else {
      val frequencyString = if (frequency == 1) {"year"} else {s"$frequency years"}
      s"every $frequencyString on $formattedMonthDay at $timeOfDayFormatted$timesToRunString"
    }
  }

  def copyWithEmptyId: Yearly = copy(id = "")

  def incrementTimesHasRun: Yearly = copy(timesHasRun = timesHasRun + 1)

  val month = monthDay.getMonthValue
  val dayOfMonth = monthDay.getDayOfMonth

  def isEarlierInYear(when: OffsetDateTime): Boolean = {
    when.getMonthValue < month ||
      (when.getMonthValue == month && when.getDayOfMonth < dayOfMonth) ||
      (when.getMonthValue == month && when.getDayOfMonth == dayOfMonth && when.toLocalTime.isBefore(timeOfDay))
  }
  def isLaterInYear(when: OffsetDateTime): Boolean = {
    when.getMonthValue > month ||
      (when.getMonthValue == month && when.getDayOfMonth > dayOfMonth) ||
      (when.getMonthValue == month && when.getDayOfMonth == dayOfMonth && when.toLocalTime.isAfter(timeOfDay))
  }

  def withAdjustments(when: OffsetDateTime): OffsetDateTime = withStandardAdjustments(when.withMonth(month).withDayOfMonth(dayOfMonth))

  protected def nextAfterAssumingZone(previous: OffsetDateTime): OffsetDateTime = {
    val yearsToAdd = if (isEarlierInYear(previous)) {
      frequency - 1
    } else {
      frequency
    }
    withAdjustments(previous.plusYears(yearsToAdd))
  }

  protected def initialAfterAssumingZone(start: OffsetDateTime): OffsetDateTime = {
    if (isLaterInYear(start)) {
      withAdjustments(start.plusYears(1))
    } else {
      withAdjustments(start)
    }
  }

  def couldRunAt(when: OffsetDateTime): Boolean = {
    val inSameTimeZone = when.atZoneSameInstant(timeZone).toOffsetDateTime
    inSameTimeZone.getMinute == timeOfDay.getMinute && inSameTimeZone.getHour == timeOfDay.getHour && inSameTimeZone.getDayOfMonth == dayOfMonth && inSameTimeZone.getMonthValue == month
  }

  val typeName = Yearly.recurrenceType
  override val maybeMonth = Some(month)
  override val maybeDayOfMonth = Some(dayOfMonth)
  override val maybeTimeOfDay = Some(timeOfDay)

}

object Yearly {
  val recurrenceType = "yearly"

  def maybeUnsavedFromText(text: String, defaultTimeZone: ZoneId): Option[Yearly] = {

    val singleRegex = """(?i).*every year.*""".r
    val nRegex = """(?i).*every\s+(\S+)\s+years?.*""".r
    val maybeYearlyFrequency = text match {
      case singleRegex() => Some(1)
      case nRegex(frequencyText) => Recurrence.maybeOrdinalFor(frequencyText, None)
      case _ => None
    }

    maybeYearlyFrequency.map { frequency =>
      Yearly(
        IDs.next,
        frequency,
        0,
        Recurrence.maybeTimesToRunFromText(text),
        Recurrence.ensureMonthDayFrom(text, defaultTimeZone),
        Recurrence.ensureTimeFrom(text, defaultTimeZone),
        defaultTimeZone
      )
    }.orElse {
      for {
        localDate <- Recurrence.maybeLocalDateFrom(text, defaultTimeZone)
        timeOfDay <- Recurrence.maybeTimeFrom(text, defaultTimeZone)
        frequency <- {
          val now = LocalDateTime.now(defaultTimeZone)
          val desiredTime = LocalDateTime.of(localDate, timeOfDay)
          /* TODO:
            The parser we use is relaxed about what information is included, so a month and day without
            a year will give you that date in the current year.

            It would be nice, however, if we differentiated between text missing the year,
            (where we can assume it means "the next [Month Day]"), and text that includes the current year,
            since some dates in the current year have already passed.

            e.g. if today is September 4, 2018, "September 3, 2018", "September 3", and "September 3, 2019"
            will all result in the same schedule. Ideally, the first one would result in nothing scheduled.
          */
          val isBeforeNow = desiredTime.isBefore(now)
          if (isBeforeNow && desiredTime.getYear < now.getYear) {
            None
          } else if (isBeforeNow) {
            Some(1)
          } else {
            var frequency = 1
            while (desiredTime.isAfter(now.plusYears(frequency)) && frequency < 10) {
              frequency = frequency + 1
            }
            if (frequency == 10 && desiredTime.isAfter(now.plusYears(frequency))) {
              None
            } else {
              Some(frequency)
            }
          }
        }
      } yield {
        val monthDay = MonthDay.of(localDate.getMonth, localDate.getDayOfMonth)
        Yearly(IDs.next, frequency, 0, Some(1), monthDay, timeOfDay, defaultTimeZone)
      }
    }
  }
}

object Recurrence {
  val runOnceRegex = """(?i).*\s+once$""".r
  val runTwiceRegex = """(?i).*\s+twice$""".r
  val runNTimesRegex = """(?i).*\s+(\d+) times?$""".r
  def maybeTimesToRunFromText(text: String): Option[Int] = {
    text.trim match {
      case runOnceRegex() => Some(1)
      case runTwiceRegex() => Some(2)
      case runNTimesRegex(n) => {
        val asInt = n.toInt
        if (asInt > 0) {
          Some(asInt)
        } else {
          None
        }
      }
      case _ => None
    }
  }

  def ordinalStringFor(i: Int): String = {
    val suffixes = Array("th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th")
    val suffix = i % 100 match {
      case 11 | 12 | 13 => "th"
      case _ => suffixes(i % 10)
    }
    s"$i$suffix"
  }

  val timeFormatter = DateTimeFormatter.ofPattern("h:mma")
  val timeFormatterWithZone = DateTimeFormatter.ofPattern("h:mma z")
  val monthDayFormatter = DateTimeFormatter.ofPattern("MMMM d")
  val dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")

  def zoneOffsetAt(when: OffsetDateTime, timeZone: ZoneId): ZoneOffset = {
    timeZone.getRules.getOffset(when.toInstant);
  }

  def withZone(when: OffsetDateTime, timeZone: ZoneId): OffsetDateTime = {
    when.withOffsetSameInstant(zoneOffsetAt(when, timeZone))
  }

  def currentAdjustedTime(timeZone: ZoneId): LocalTime = {
    withZone(OffsetDateTime.now, timeZone).toLocalTime.withSecond(0).withNano(0)
  }

  def currentMonthDay(timeZone: ZoneId): MonthDay = {
    val now = withZone(OffsetDateTime.now, timeZone)
    MonthDay.of(now.getMonth, now.getDayOfMonth)
  }

  private def maybeDateFrom(text: String, defaultTimeZone: ZoneId): Option[Date] = {
    val parser = new Parser(TimeZone.getTimeZone(defaultTimeZone))
    val groups = parser.parse(text)
    if (groups.isEmpty || groups.get(0).getDates.isEmpty) {
      None
    } else {
      Some(groups.get(0).getDates.get(0))
    }
  }

  private def maybeCalendarFrom(text: String, defaultTimeZone: ZoneId): Option[Calendar] = {
    val onDateAndTimeRegex = """(?i).*on\s+(.*?)(at.*)?$""".r
    text match {
      case onDateAndTimeRegex(date, _) => maybeDateFrom(date, defaultTimeZone).map { date =>
        val calendar = Calendar.getInstance(TimeZone.getTimeZone(defaultTimeZone))
        calendar.setTime(date)
        calendar
      }
      case _ => None
    }
  }


  def maybeMonthDayFrom(text: String, defaultTimeZone: ZoneId): Option[MonthDay] = {
    maybeCalendarFrom(text, defaultTimeZone).map { calendar =>
      val javaMonth = calendar.get(Calendar.MONTH)
      val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
      MonthDay.of(javaMonth + 1, dayOfMonth)
    }
  }

  def maybeLocalDateFrom(text: String, defaultTimeZone: ZoneId): Option[LocalDate] = {
    maybeCalendarFrom(text, defaultTimeZone).map { calendar =>
      val year = calendar.get(Calendar.YEAR)
      val javaMonth = calendar.get(Calendar.MONTH)
      val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
      LocalDate.of(year, javaMonth + 1, dayOfMonth)
    }
  }

  def ensureMonthDayFrom(text: String, defaultTimeZone: ZoneId): MonthDay = {
    maybeMonthDayFrom(text, defaultTimeZone).getOrElse(currentMonthDay(defaultTimeZone))
  }

  def maybeTimeFrom(text: String, defaultTimeZone: ZoneId): Option[LocalTime] = {
    val timeRegex = """(?i).*at\s+(.*)""".r
    text match {
      case timeRegex(time) => maybeDateFrom(time, defaultTimeZone).map { date =>
        val dateTime = OffsetDateTime.ofInstant(date.toInstant, defaultTimeZone)
        dateTime.toLocalTime
      }
      case _ => None
    }
  }

  def ensureTimeFrom(text: String, defaultTimeZone: ZoneId): LocalTime = {
    maybeTimeFrom(text, defaultTimeZone).getOrElse(currentAdjustedTime(defaultTimeZone))
  }

  def dayOfWeekNameFor(dayOfWeek: DayOfWeek): String = dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)

  def dayOfWeekRegexFor(dayNames: String*): Regex = {
    s"""(?i).*\\b${dayNames.mkString("|")}\\b.*""".r
  }
  val mondayRegex: Regex = dayOfWeekRegexFor("monday", "mon")
  val tuesdayRegex: Regex = dayOfWeekRegexFor("tuesday", "tues")
  val wednesdayRegex: Regex = dayOfWeekRegexFor("wednesday", "wed")
  val thursdayRegex: Regex = dayOfWeekRegexFor("thursday", "thurs")
  val fridayRegex: Regex = dayOfWeekRegexFor("friday", "fri")
  val saturdayRegex: Regex = dayOfWeekRegexFor("saturday", "sat")
  val sundayRegex: Regex = dayOfWeekRegexFor("sunday", "sun")

  def includesDayOfWeek(text: String): Boolean = {
    daysOfWeekFrom(text).nonEmpty
  }

  def maybeDayOfWeekFrom(text: String): Option[DayOfWeek] = {
    daysOfWeekFrom(text).headOption
  }

  case class DayOfWeekMatcher(regex: Regex, dayOfWeek: DayOfWeek) {
    def process(text: String, buffer: ArrayBuffer[DayOfWeek]): Unit = {
      regex.findFirstMatchIn(text).foreach(_ => buffer += dayOfWeek)
    }
  }

  val dayOfWeekMatchers = Seq(
    DayOfWeekMatcher(mondayRegex, DayOfWeek.MONDAY),
    DayOfWeekMatcher(tuesdayRegex, DayOfWeek.TUESDAY),
    DayOfWeekMatcher(wednesdayRegex, DayOfWeek.WEDNESDAY),
    DayOfWeekMatcher(thursdayRegex, DayOfWeek.THURSDAY),
    DayOfWeekMatcher(fridayRegex, DayOfWeek.FRIDAY),
    DayOfWeekMatcher(saturdayRegex, DayOfWeek.SATURDAY),
    DayOfWeekMatcher(sundayRegex, DayOfWeek.SUNDAY)
  )

  def daysOfWeekFrom(text: String): Seq[DayOfWeek] = {
    val days = ArrayBuffer[DayOfWeek]()
    if ("every weekday".r.findFirstMatchIn(text).nonEmpty) {
      days ++= Seq(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
    } else {
      dayOfWeekMatchers.foreach(_.process(text, days))
    }
    days
  }

  def maybeMonthlyFrequencyFrom(text: String): Option[Int] = {
    val singleRegex = """(?i).*every month.*""".r
    val nRegex = """(?i).*every\s+(\S+)\s+months?.*""".r
    text match {
      case singleRegex() => Some(1)
      case nRegex(frequencyText) => Recurrence.maybeOrdinalFor(frequencyText, None)
      case _ => None
    }
  }

  val numberWithDigitsRegex = """(?i)(\d+)(st|nd|rd|th)?""".r

  val ordinalMappings = Seq(
    ("first|1st", 1),
    ("second|2nd", 2),
    ("third|3rd", 3),
    ("fourth|4th", 4),
    ("fifth|5th", 5),
    ("sixth|6th", 6),
    ("seventh|7th", 7),
    ("eighth|8th", 8),
    ("ninth|9th", 9),
    ("tenth|10th", 10),
    ("eleventh|11th", 11),
    ("twelfth|12th", 12),
    ("thirteenth|13th", 13),
    ("fourteenth|14th", 14),
    ("fifteenth|15th", 15),
    ("sixteenth|16th", 16),
    ("seventeeth|17th", 17),
    ("eighteent|18th", 18),
    ("nineteenth|19th", 19),
    ("twentieth|20th", 20),
    ("twentyfirst|21st", 21),
    ("twentysecond|22nd", 22),
    ("twentythird|23rd", 23),
    ("twentyfourth|24th", 24),
    ("twentyfifth|25th", 25),
    ("twentysixth|26th", 26),
    ("twentyseventh|27th", 27),
    ("twentyeighth|28th", 28),
    ("twentyninth|29th", 29),
    ("thirtieth|30th", 30),
    ("thirtyfirst|31st", 31)
  ).reverse

  private def ordinalRegexFor(ordinal: String, maybeExcludingNext: Option[String]): Regex = {
    val excludingPattern = maybeExcludingNext.map(v => s"(?!$v)").getOrElse("")
    s"""(?i).*$ordinal$excludingPattern.*""".r
  }

  def maybeOrdinalFor(text: String, maybeExcludingNext: Option[String]): Option[Int] = {
    ordinalMappings.find { case(pattern, i) =>
      ordinalRegexFor(pattern, maybeExcludingNext).findFirstMatchIn(text).nonEmpty
    }.map { case(pattern, i) => i }
  }

  private def buildFrom(
                       id: String,
                       recurrenceType: String,
                       frequency: Int,
                       timesHasRun: Int,
                       maybeTimesToRun: Option[Int],
                       daysOfWeek: Seq[DayOfWeek],
                       maybeTimeOfDay: Option[LocalTime],
                       timeZone: ZoneId,
                       maybeMinuteOfHour: Option[Int],
                       maybeDayOfWeek: Option[DayOfWeek],
                       maybeDayOfMonth: Option[Int],
                       maybeNthDayOfWeek: Option[Int],
                       maybeMonth: Option[Int]
                       ): Recurrence = {
    recurrenceType match {
      case(Minutely.recurrenceType) => Minutely(id, frequency, timesHasRun, maybeTimesToRun)
      case(Hourly.recurrenceType) => Hourly(id, frequency, timesHasRun, maybeTimesToRun, maybeMinuteOfHour.get, timeZone)
      case(Daily.recurrenceType) => Daily(id, frequency, timesHasRun, maybeTimesToRun, maybeTimeOfDay.get, timeZone)
      case(Weekly.recurrenceType) => Weekly(id, frequency, timesHasRun, maybeTimesToRun, daysOfWeek, maybeTimeOfDay.get, timeZone)
      case(MonthlyByDayOfMonth.recurrenceType) => MonthlyByDayOfMonth(id, frequency, timesHasRun, maybeTimesToRun, maybeDayOfMonth.get, maybeTimeOfDay.get, timeZone)
      case(MonthlyByNthDayOfWeek.recurrenceType) => MonthlyByNthDayOfWeek(id, frequency, timesHasRun, maybeTimesToRun, maybeDayOfWeek.get, maybeNthDayOfWeek.get, maybeTimeOfDay.get, timeZone)
      case(Yearly.recurrenceType) => Yearly(id, frequency, timesHasRun, maybeTimesToRun, MonthDay.of(maybeMonth.get, maybeDayOfMonth.get), maybeTimeOfDay.get, timeZone)
    }
  }

  def buildFor(raw: RawRecurrence, defaultTimeZone: ZoneId): Recurrence = {
    buildFrom(
      raw.id,
      raw.recurrenceType,
      raw.frequency,
      raw.timesHasRun,
      raw.maybeTotalTimesToRun,
      raw.daysOfWeek,
      raw.maybeTimeOfDay,
      raw.maybeTimeZone.getOrElse(defaultTimeZone),
      raw.maybeMinuteOfHour,
      raw.maybeDayOfWeek,
      raw.maybeDayOfMonth,
      raw.maybeNthDayOfWeek,
      raw.maybeMonth
    )
  }

  def maybeUnsavedFromText(text: String, defaultTimeZone: ZoneId): Option[Recurrence] = {
    Minutely.maybeUnsavedFromText(text).orElse {
      Hourly.maybeUnsavedFromText(text, defaultTimeZone).orElse {
        Daily.maybeUnsavedFromText(text, defaultTimeZone).orElse {
          Weekly.maybeUnsavedFromText(text, defaultTimeZone).orElse {
            MonthlyByDayOfMonth.maybeUnsavedFromText(text, defaultTimeZone).orElse {
              MonthlyByNthDayOfWeek.maybeUnsavedFromText(text, defaultTimeZone).orElse {
                Yearly.maybeUnsavedFromText(text, defaultTimeZone).orElse {
                  None
                }
              }
            }
          }
        }
      }
    }
  }
}
