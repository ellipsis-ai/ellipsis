package models.behaviors.scheduledmessage

import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.{Calendar, Date, Locale}

import com.joestelmach.natty._
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, DateTimeZone, LocalTime, MonthDay}

import scala.collection.mutable.ArrayBuffer
import scala.util.matching.Regex

sealed trait Recurrence {
  val frequency: Int
  val typeName: String
  val maybeTimeOfDay: Option[LocalTime] = None
  val maybeTimeZone: Option[DateTimeZone] = None
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
  private def withZone(when: DateTime): DateTime = {
    maybeTimeZone.map { timeZone =>
      when.withZone(timeZone)
    }.getOrElse(when)
  }
  protected def nextAfterAssumingZone(previous: DateTime): DateTime
  def nextAfter(previous: DateTime): DateTime = {
    nextAfterAssumingZone(withZone(previous))
  }
  protected def initialAfterAssumingZone(start: DateTime): DateTime
  def initialAfter(start: DateTime): DateTime = {
    initialAfterAssumingZone(withZone(start))
  }
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

  protected def nextAfterAssumingZone(previous: DateTime): DateTime = {
    val hoursToAdd = if (isEarlierInHour(previous)) {
      frequency - 1
    } else {
      frequency
    }
    withAdjustments(previous.plusHours(hoursToAdd))
  }

  protected def initialAfterAssumingZone(start: DateTime): DateTime = {
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

trait RecurrenceWithTimeOfDay extends Recurrence {
  val timeOfDay: LocalTime
  val timeZone: DateTimeZone
  override val maybeTimeZone = Some(timeZone)
  val hourOfDay = timeOfDay.getHourOfDay
  val minuteOfHour = timeOfDay.getMinuteOfHour
  val secondOfMinute = timeOfDay.getSecondOfMinute
  val millisOfSecond = timeOfDay.getMillisOfSecond

  def stringFor(timeZone: DateTimeZone): String = s"(${timeZone.toString})"

  override def withStandardAdjustments(when: DateTime): DateTime = {
    super.withStandardAdjustments(withTime(when))
  }

  def withTime(when: DateTime): DateTime = {
    when.withZone(timeZone).withTime(hourOfDay, minuteOfHour, secondOfMinute, millisOfSecond)
  }
}

case class Daily(frequency: Int, timeOfDay: LocalTime, timeZone: DateTimeZone) extends RecurrenceWithTimeOfDay {

  override def displayString: String = {
    val frequencyString = if (frequency == 1) { "day" } else { s"$frequency days" }
    s"every $frequencyString at ${timeOfDay.toString(Recurrence.timeFormatter)} ${stringFor(timeZone)}"
  }

  def isEarlierInDay(when: DateTime): Boolean = when.toLocalTime.isBefore(timeOfDay)
  def isLaterInDay(when: DateTime): Boolean = when.toLocalTime.isAfter(timeOfDay)

  def withAdjustments(when: DateTime): DateTime = withStandardAdjustments(when)

  protected def nextAfterAssumingZone(previous: DateTime): DateTime = {
    val daysToAdd = if (isEarlierInDay(previous)) {
      frequency - 1
    } else {
      frequency
    }
    withAdjustments(previous.plusDays(daysToAdd))
  }

  protected def initialAfterAssumingZone(start: DateTime): DateTime = {
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

  def maybeFromText(text: String, defaultTimeZone: DateTimeZone): Option[Daily] = {
    val singleRegex = """(?i)every day.*""".r
    val nRegex = """(?i)every\s+(\d+)\s+days?.*""".r
    val maybeFrequency = text match {
      case singleRegex() => Some(1)
      case nRegex(frequency) => Some(frequency.toInt)
      case _ => None
    }
    maybeFrequency.map { frequency =>
      val maybeTime = Recurrence.maybeTimeFrom(text, defaultTimeZone)
      Daily(frequency, maybeTime.getOrElse(Recurrence.currentAdjustedTime(defaultTimeZone)), defaultTimeZone)
    }
  }
}

case class Weekly(
                   frequency: Int,
                   override val daysOfWeek: Seq[DayOfWeek],
                   timeOfDay: LocalTime,
                   timeZone: DateTimeZone
                 ) extends RecurrenceWithTimeOfDay {

  lazy val daysOfWeekValues = daysOfWeek.map(_.getValue)

  lazy val daysOfWeekString = {
    daysOfWeek.map(Recurrence.dayOfWeekNameFor).mkString(", ")
  }

  def maybeNextDayInWeekOf(when: DateTime): Option[Int] = {
    if (isEarlierTheSameDay(when)) {
      Some(when.getDayOfWeek)
    } else {
      daysOfWeekValues.find(ea => ea > when.getDayOfWeek)
    }
  }

  def nextDayOfWeekFor(when: DateTime): Int = {
    maybeNextDayInWeekOf(when).getOrElse(daysOfWeekValues.head)
  }

  override def displayString: String = {
    val frequencyString = if (frequency == 1) { "week" } else { s"$frequency weeks" }
    s"every $frequencyString on $daysOfWeekString at ${timeOfDay.toString(Recurrence.timeFormatter)} ${stringFor(timeZone)}"
  }

  def isEarlierTheSameDay(when: DateTime): Boolean = {
    daysOfWeekValues.contains(when.getDayOfWeek) && when.toLocalTime.isBefore(timeOfDay)
  }

  def isEarlierInWeek(when: DateTime): Boolean = {
    maybeNextDayInWeekOf(when).isDefined
  }
  def isLaterInWeek(when: DateTime): Boolean = !isEarlierInWeek(when)

  def withAdjustments(when: DateTime): DateTime = {
    withStandardAdjustments(when.withDayOfWeek(nextDayOfWeekFor(when)))
  }

  protected def nextAfterAssumingZone(previous: DateTime): DateTime = {
    val weeksToAdd = if (isEarlierInWeek(previous)) {
      frequency - 1
    } else {
      frequency
    }
    withAdjustments(previous.plusWeeks(weeksToAdd))
  }

  protected def initialAfterAssumingZone(start: DateTime): DateTime = {
    if (isLaterInWeek(start)) {
      withAdjustments(start.plusWeeks(1))
    } else {
      withAdjustments(start)
    }
  }

  val typeName = Weekly.recurrenceType
  override val maybeTimeOfDay = Some(timeOfDay)

}

object Weekly {
  val recurrenceType = "weekly"

  def maybeFromText(text: String, defaultTimeZone: DateTimeZone): Option[Weekly] = {
    val singleRegex = """(?i)every week.*""".r
    val nRegex = """(?i)every\s+(\d+)\s+weeks?.*""".r
    val maybeFrequency = text match {
      case singleRegex() => Some(1)
      case nRegex(frequency) => Some(frequency.toInt)
      case _ => None
    }
    maybeFrequency.map { frequency =>
      var daysOfWeek = Recurrence.daysOfWeekFrom(text)
      if (daysOfWeek.isEmpty) {
        daysOfWeek = Seq(DateTime.now.getDayOfWeek)
      }
      val maybeTime = Recurrence.maybeTimeFrom(text, defaultTimeZone)
      Weekly(
        frequency,
        daysOfWeek.map(DayOfWeek.of),
        maybeTime.getOrElse(Recurrence.currentAdjustedTime(defaultTimeZone)),
        defaultTimeZone
      )
    }
  }
}

case class MonthlyByDayOfMonth(frequency: Int, dayOfMonth: Int, timeOfDay: LocalTime, timeZone: DateTimeZone) extends RecurrenceWithTimeOfDay {

  override def displayString: String = {
    val frequencyString = if (frequency == 1) { "month" } else { s"$frequency months" }
    s"every $frequencyString on the ${Recurrence.ordinalStringFor(dayOfMonth)} at ${timeOfDay.toString(Recurrence.timeFormatter)} ${stringFor(timeZone)}"
  }

  def isEarlierInMonth(when: DateTime): Boolean = {
    when.getDayOfMonth < dayOfMonth || (when.getDayOfMonth == dayOfMonth && when.toLocalTime.isBefore(timeOfDay))
  }
  def isLaterInMonth(when: DateTime): Boolean = {
    when.getDayOfMonth > dayOfMonth || (when.getDayOfMonth == dayOfMonth && when.toLocalTime.isAfter(timeOfDay))
  }

  def withAdjustments(when: DateTime): DateTime = withStandardAdjustments(when.withDayOfMonth(dayOfMonth))

  protected def nextAfterAssumingZone(previous: DateTime): DateTime = {
    val monthsToAdd = if (isEarlierInMonth(previous)) {
      frequency - 1
    } else {
      frequency
    }
    withAdjustments(previous.plusMonths(monthsToAdd))
  }

  protected def initialAfterAssumingZone(start: DateTime): DateTime = {
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

  def maybeFromText(text: String, defaultTimeZone: DateTimeZone): Option[MonthlyByDayOfMonth] = {
    if (Recurrence.includesDayOfWeek(text)) {
      return None
    }

    Recurrence.maybeMonthlyFrequencyFrom(text).map { frequency =>
      val maybeDayOfMonth = maybeDayOfMonthFrom(text)
      MonthlyByDayOfMonth(
        frequency,
        maybeDayOfMonth.getOrElse(DateTime.now.getDayOfMonth),
        Recurrence.ensureTimeFrom(text, defaultTimeZone),
        defaultTimeZone
      )
    }
  }
}

case class MonthlyByNthDayOfWeek(frequency: Int, dayOfWeek: DayOfWeek, nth: Int, timeOfDay: LocalTime, timeZone: DateTimeZone) extends RecurrenceWithTimeOfDay {

  override def displayString: String = {
    val frequencyString = if (frequency == 1) { "month" } else { s"$frequency months" }
    s"every $frequencyString on the ${Recurrence.ordinalStringFor(nth)} ${Recurrence.dayOfWeekNameFor(dayOfWeek)} at ${timeOfDay.toString(Recurrence.timeFormatter)} ${stringFor(timeZone)}"
  }

  def targetInMonthMatching(when: DateTime): DateTime = {
    val firstOfTheMonth = when.withDayOfMonth(1)
    val weeksToAdd = if (firstOfTheMonth.getDayOfWeek <= dayOfWeek.getValue) {
      nth - 1
    } else {
      nth
    }
    withStandardAdjustments(firstOfTheMonth.plusWeeks(weeksToAdd).withDayOfWeek(dayOfWeek.getValue))
  }

  protected def nextAfterAssumingZone(previous: DateTime): DateTime = {
    val monthsToAdd = if (targetInMonthMatching(previous).isAfter(previous)) {
      frequency - 1
    } else {
      frequency
    }
    targetInMonthMatching(previous.plusMonths(monthsToAdd))
  }

  protected def initialAfterAssumingZone(start: DateTime): DateTime = {
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

case class NthDayOfWeek(dayOfWeek: DayOfWeek, n: Int)

object MonthlyByNthDayOfWeek {
  val recurrenceType = "monthly_by_nth_day_of_week"

  private def maybeNthDayOfWeekFrom(text: String): Option[NthDayOfWeek] = {
    for {
      dayOfWeek <- Recurrence.maybeDayOfWeekFrom(text).map(DayOfWeek.of)
      ordinal <- Recurrence.maybeOrdinalFor(text, Some(" month"))
    } yield NthDayOfWeek(dayOfWeek, ordinal)
  }

  def maybeFromText(text: String, defaultTimeZone: DateTimeZone): Option[MonthlyByNthDayOfWeek] = {
    for {
      nthDayOfWeek <- maybeNthDayOfWeekFrom(text)
      frequency <- Recurrence.maybeMonthlyFrequencyFrom(text)
    } yield {
      MonthlyByNthDayOfWeek(
        frequency,
        nthDayOfWeek.dayOfWeek,
        nthDayOfWeek.n,
        Recurrence.ensureTimeFrom(text, defaultTimeZone),
        defaultTimeZone
      )
    }
  }
}

case class Yearly(frequency: Int, monthDay: MonthDay, timeOfDay: LocalTime, timeZone: DateTimeZone) extends RecurrenceWithTimeOfDay {

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

  def withAdjustments(when: DateTime): DateTime = withStandardAdjustments(when.withMonthOfYear(month).withDayOfMonth(dayOfMonth))

  protected def nextAfterAssumingZone(previous: DateTime): DateTime = {
    val yearsToAdd = if (isEarlierInYear(previous)) {
      frequency - 1
    } else {
      frequency
    }
    withAdjustments(previous.plusYears(yearsToAdd))
  }

  protected def initialAfterAssumingZone(start: DateTime): DateTime = {
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

  def maybeFromText(text: String, defaultTimeZone: DateTimeZone): Option[Yearly] = {

    val singleRegex = """(?i).*every year.*""".r
    val nRegex = """(?i).*every\s+(\S+)\s+years?.*""".r
    val maybeYearlyFrequency = text match {
      case singleRegex() => Some(1)
      case nRegex(frequencyText) => Recurrence.maybeOrdinalFor(frequencyText, None)
      case _ => None
    }

    maybeYearlyFrequency.map { frequency =>
      Yearly(
        frequency,
        Recurrence.ensureMonthDayFrom(text, defaultTimeZone),
        Recurrence.ensureTimeFrom(text, defaultTimeZone),
        defaultTimeZone
      )
    }
  }
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

  val timeFormatter = DateTimeFormat.forPattern("h:mma")
  val timeFormatterWithZone = DateTimeFormat.forPattern("h:mma z")

  def currentAdjustedTime(timeZone: DateTimeZone): LocalTime = {
    DateTime.now.withZone(timeZone).toLocalTime.withSecondOfMinute(0).withMillisOfSecond(0)
  }
  def currentMonthDay(timeZone: DateTimeZone): MonthDay = {
    val now = DateTime.now.withZone(timeZone)
    new MonthDay(now.getMonthOfYear, now.getDayOfMonth)
  }

  private def maybeDateFrom(text: String, defaultTimeZone: DateTimeZone): Option[Date] = {
    val parser = new Parser(defaultTimeZone.toTimeZone)
    val groups = parser.parse(text)
    if (groups.isEmpty || groups.get(0).getDates.isEmpty) {
      None
    } else {
      Some(groups.get(0).getDates.get(0))
    }
  }

  def maybeMonthDayFrom(text: String, defaultTimeZone: DateTimeZone): Option[MonthDay] = {
    val monthDayRegex = """(?i).*on\s+(.*?)(at.*)?$""".r
    text match {
      case monthDayRegex(monthDay, _) => maybeDateFrom(monthDay, defaultTimeZone).map { date =>
        val calendar = Calendar.getInstance(defaultTimeZone.toTimeZone)
        calendar.setTime(date)
        val javaMonth = calendar.get(Calendar.MONTH)
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        new MonthDay(javaMonth + 1, dayOfMonth)
      }
      case _ => None
    }
  }

  def ensureMonthDayFrom(text: String, defaultTimeZone: DateTimeZone): MonthDay = {
    maybeMonthDayFrom(text, defaultTimeZone).getOrElse(currentMonthDay(defaultTimeZone))
  }

  def maybeTimeFrom(text: String, defaultTimeZone: DateTimeZone): Option[LocalTime] = {
    val timeRegex = """(?i).*at\s+(.*)""".r
    text match {
      case timeRegex(time) => maybeDateFrom(time, defaultTimeZone).map { date =>
        val dateTime = new DateTime(date).withZone(defaultTimeZone)
        dateTime.toLocalTime
      }
      case _ => None
    }
  }

  def ensureTimeFrom(text: String, defaultTimeZone: DateTimeZone): LocalTime = {
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

  def maybeDayOfWeekFrom(text: String): Option[Int] = {
    daysOfWeekFrom(text).headOption
  }

  case class DayOfWeekMatcher(regex: Regex, dayOfWeek: DayOfWeek) {
    def process(text: String, buffer: ArrayBuffer[Int]): Unit = {
      regex.findFirstMatchIn(text).foreach(_ => buffer += dayOfWeek.getValue)
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

  def daysOfWeekFrom(text: String): Seq[Int] = {
    val days = ArrayBuffer[Int]()
    dayOfWeekMatchers.foreach(_.process(text, days))
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
                 recurrenceType: String,
                 frequency: Int,
                 daysOfWeek: Seq[DayOfWeek],
                 maybeTimeOfDay: Option[LocalTime],
                 timeZone: DateTimeZone,
                 maybeMinuteOfHour: Option[Int],
                 maybeDayOfWeek: Option[DayOfWeek],
                 maybeDayOfMonth: Option[Int],
                 maybeNthDayOfWeek: Option[Int],
                 maybeMonth: Option[Int]
                 ): Recurrence = {
    recurrenceType match {
      case(Hourly.recurrenceType) => Hourly(frequency, maybeMinuteOfHour.get)
      case(Daily.recurrenceType) => Daily(frequency, maybeTimeOfDay.get, timeZone)
      case(Weekly.recurrenceType) => Weekly(frequency, daysOfWeek, maybeTimeOfDay.get, timeZone)
      case(MonthlyByDayOfMonth.recurrenceType) => MonthlyByDayOfMonth(frequency, maybeDayOfMonth.get, maybeTimeOfDay.get, timeZone)
      case(MonthlyByNthDayOfWeek.recurrenceType) => MonthlyByNthDayOfWeek(frequency, maybeDayOfWeek.get, maybeNthDayOfWeek.get, maybeTimeOfDay.get, timeZone)
      case(Yearly.recurrenceType) => Yearly(frequency, new MonthDay(maybeMonth.get, maybeDayOfMonth.get), maybeTimeOfDay.get, timeZone)
    }
  }

  def buildFor(raw: RawScheduledMessage, defaultTimeZone: DateTimeZone): Recurrence = {
    buildFrom(
      raw.recurrenceType,
      raw.frequency,
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

  def maybeFromText(text: String, defaultTimeZone: DateTimeZone): Option[Recurrence] = {
    Hourly.maybeFromText(text).orElse {
      Daily.maybeFromText(text, defaultTimeZone).orElse {
        Weekly.maybeFromText(text, defaultTimeZone).orElse {
          MonthlyByDayOfMonth.maybeFromText(text, defaultTimeZone).orElse {
            MonthlyByNthDayOfWeek.maybeFromText(text, defaultTimeZone).orElse {
              Yearly.maybeFromText(text, defaultTimeZone).orElse {
                None
              }
            }
          }
        }
      }
    }
  }
}
