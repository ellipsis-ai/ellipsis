package models.bots

import java.text.{ParseException, SimpleDateFormat}
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.{Calendar, Locale, Date}
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, LocalTime, MonthDay}
import com.joestelmach.natty._

import scala.util.matching.Regex

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
  def displayString: String = ""
}
case class Hourly(frequency: Int, minuteOfHour: Int) extends Recurrence {

  override def displayString: String = {
    val frequencyString = if (frequency == 1) { "hour" } else { s"$frequency hours" }
    s"every $frequencyString at $minuteOfHour minutes"
  }

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
    val singleRegex = """(?i).*every hour.*""".r
    val nRegex = """(?i).*every\s+(\d+)\s+hours?.*""".r
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

  override def displayString: String = {
    val frequencyString = if (frequency == 1) { "day" } else { s"$frequency days" }
    s"every $frequencyString at ${timeOfDay.toString(Recurrence.timeFormatter)}"
  }

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
      val maybeTime = Recurrence.maybeTimeFrom(text)
      Daily(frequency, maybeTime.getOrElse(Recurrence.currentAdjustedTime))
    }
  }
}

case class Weekly(frequency: Int, dayOfWeek: Int, timeOfDay: LocalTime) extends Recurrence {

  val dayOfWeekName = DayOfWeek.of(dayOfWeek).getDisplayName(TextStyle.FULL, Locale.ENGLISH)

  override def displayString: String = {
    val frequencyString = if (frequency == 1) { "week" } else { s"$frequency weeks" }
    s"every $frequencyString on $dayOfWeekName at ${timeOfDay.toString(Recurrence.timeFormatter)}"
  }

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
      val maybeDayOfWeek = Recurrence.maybeDayOfWeekFrom(text)
      val maybeTime = Recurrence.maybeTimeFrom(text)
      Weekly(frequency, maybeDayOfWeek.getOrElse(DateTime.now.getDayOfWeek), maybeTime.getOrElse(Recurrence.currentAdjustedTime))
    }
  }
}

case class MonthlyByDayOfMonth(frequency: Int, dayOfMonth: Int, timeOfDay: LocalTime) extends Recurrence {

  override def displayString: String = {
    val frequencyString = if (frequency == 1) { "month" } else { s"$frequency months" }
    s"every $frequencyString on the ${Recurrence.ordinalStringFor(dayOfMonth)} at ${timeOfDay.toString(Recurrence.timeFormatter)}"
  }

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

  private def maybeDayOfMonthFrom(text: String): Option[Int] = {
    Recurrence.maybeOrdinalFor(text, Some(" month"))
  }

  def maybeFromText(text: String): Option[MonthlyByDayOfMonth] = {
    if (Recurrence.includesDayOfWeek(text)) {
      return None
    }

    Recurrence.maybeMonthlyFrequencyFrom(text).map { frequency =>
      val maybeDayOfMonth = maybeDayOfMonthFrom(text)
      MonthlyByDayOfMonth(frequency, maybeDayOfMonth.getOrElse(DateTime.now.getDayOfMonth), Recurrence.ensureTimeFrom(text))
    }
  }
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

case class NthDayOfWeek(dayOfWeek: Int, n: Int)

object MonthlyByNthDayOfWeek {
  val recurrenceType = "monthly_by_nth_day_of_week"

  private def maybeNthDayOfWeekFrom(text: String): Option[NthDayOfWeek] = {
    for {
      dayOfWeek <- Recurrence.maybeDayOfWeekFrom(text)
      ordinal <- Recurrence.maybeOrdinalFor(text, Some(" month"))
    } yield NthDayOfWeek(dayOfWeek, ordinal)
  }

  def maybeFromText(text: String): Option[MonthlyByNthDayOfWeek] = {
    for {
      nthDayOfWeek <- maybeNthDayOfWeekFrom(text)
      frequency <- Recurrence.maybeMonthlyFrequencyFrom(text)
    } yield {
      MonthlyByNthDayOfWeek(frequency, nthDayOfWeek.dayOfWeek, nthDayOfWeek.n, Recurrence.ensureTimeFrom(text))
    }
  }
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

  def ordinalStringFor(i: Int): String = {
    val suffixes = Array("th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th")
    val suffix = i % 100 match {
      case 11 | 12 | 13 => "th"
      case _ => suffixes(i % 10)
    }
    s"$i$suffix"
  }

  val timeFormatter = DateTimeFormat.forPattern("h:mma z")

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
    val timeRegex = """(?i).*at\s+(.*)""".r
    text match {
      case timeRegex(time) => maybeDateFrom(time).map { date =>
        new LocalTime(date.getTime)
      }
      case _ => None
    }
  }

  def ensureTimeFrom(text: String): LocalTime = {
    maybeTimeFrom(text).getOrElse(currentAdjustedTime)
  }

  val daysOfWeekRegex = """(?i).*(monday|tuesday|wednesday|thursday|friday|saturday|sunday).*""".r
  def includesDayOfWeek(text: String): Boolean = {
    daysOfWeekRegex.findFirstMatchIn(text).nonEmpty
  }

  def parseDayOfWeekFrom(dayOfWeekName: String): Option[Int] = {
    try {
      val dayFormat = new SimpleDateFormat("E", Locale.US)
      val date = dayFormat.parse(dayOfWeekName)
      val calendar = Calendar.getInstance()
      calendar.setTime(date)
      val javaDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
      val translatedToJoda = if (javaDayOfWeek == 1) { 7 } else { javaDayOfWeek - 1 }
      Some(translatedToJoda)
    } catch {
      case e: ParseException => None
    }
  }

  def maybeDayOfWeekFrom(text: String): Option[Int] = {
    text match {
      case daysOfWeekRegex(day) => parseDayOfWeekFrom(day)
      case _ => None
    }
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
          MonthlyByDayOfMonth.maybeFromText(text).orElse {
            MonthlyByNthDayOfWeek.maybeFromText(text).orElse {
              None
            }
          }
        }
      }
    }
  }
}
