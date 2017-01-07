package models.behaviors.scheduledmessage

import java.time._
import java.time.format.{DateTimeFormatter, TextStyle}
import java.util.{Calendar, Date, Locale, TimeZone}

import com.joestelmach.natty._

import scala.collection.mutable.ArrayBuffer
import scala.util.matching.Regex

sealed trait Recurrence {
  val frequency: Int
  val typeName: String
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
  def displayString: String = ""
}
case class Hourly(frequency: Int, minuteOfHour: Int) extends Recurrence {

  override def displayString: String = {
    val frequencyString = if (frequency == 1) { "hour" } else { s"$frequency hours" }
    s"every $frequencyString at $minuteOfHour minutes"
  }

  def isEarlierInHour(when: OffsetDateTime): Boolean = when.getMinute < minuteOfHour
  def isLaterInHour(when: OffsetDateTime): Boolean = when.getMinute > minuteOfHour

  def withAdjustments(when: OffsetDateTime): OffsetDateTime = withStandardAdjustments(when.withMinute(minuteOfHour))

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
      Hourly(frequency, maybeMinuteOfHour.getOrElse(OffsetDateTime.now.getMinute))
    }
  }
}

trait RecurrenceWithTimeOfDay extends Recurrence {
  val timeOfDay: LocalTime
  val timeZone: ZoneId
  override val maybeTimeZone = Some(timeZone)
  val hourOfDay = timeOfDay.getHour
  val minuteOfHour = timeOfDay.getMinute
  val secondOfMinute = timeOfDay.getSecond
  val nanosOfSecond = timeOfDay.getNano

  def stringFor(timeZone: ZoneId): String = s"(${timeZone.toString})"

  override def withStandardAdjustments(when: OffsetDateTime): OffsetDateTime = {
    super.withStandardAdjustments(withTime(when))
  }

  def withTime(when: OffsetDateTime): OffsetDateTime = {
    withZone(when).withHour(hourOfDay).withMinute(minuteOfHour).withSecond(secondOfMinute).withNano(nanosOfSecond)
  }
}

case class Daily(frequency: Int, timeOfDay: LocalTime, timeZone: ZoneId) extends RecurrenceWithTimeOfDay {

  override def displayString: String = {
    val frequencyString = if (frequency == 1) { "day" } else { s"$frequency days" }
    s"every $frequencyString at ${timeOfDay.format(Recurrence.timeFormatter)} ${stringFor(timeZone)}"
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

}

object Daily {
  val recurrenceType = "daily"

  def maybeFromText(text: String, defaultTimeZone: ZoneId): Option[Daily] = {
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
                   timeZone: ZoneId
                 ) extends RecurrenceWithTimeOfDay {

  lazy val daysOfWeekValues = daysOfWeek.map(_.getValue)

  lazy val daysOfWeekString = {
    daysOfWeek.map(Recurrence.dayOfWeekNameFor).mkString(", ")
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

  override def displayString: String = {
    val frequencyString = if (frequency == 1) { "week" } else { s"$frequency weeks" }
    s"every $frequencyString on $daysOfWeekString at ${Recurrence.timeFormatter.format(timeOfDay)} ${stringFor(timeZone)}"
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

  val typeName = Weekly.recurrenceType
  override val maybeTimeOfDay = Some(timeOfDay)

}

object Weekly {
  val recurrenceType = "weekly"

  def maybeFromText(text: String, defaultTimeZone: ZoneId): Option[Weekly] = {
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
        daysOfWeek = Seq(OffsetDateTime.now.getDayOfWeek)
      }
      val maybeTime = Recurrence.maybeTimeFrom(text, defaultTimeZone)
      Weekly(
        frequency,
        daysOfWeek,
        maybeTime.getOrElse(Recurrence.currentAdjustedTime(defaultTimeZone)),
        defaultTimeZone
      )
    }
  }
}

case class MonthlyByDayOfMonth(frequency: Int, dayOfMonth: Int, timeOfDay: LocalTime, timeZone: ZoneId) extends RecurrenceWithTimeOfDay {

  override def displayString: String = {
    val frequencyString = if (frequency == 1) { "month" } else { s"$frequency months" }
    s"every $frequencyString on the ${Recurrence.ordinalStringFor(dayOfMonth)} at ${timeOfDay.format(Recurrence.timeFormatter)} ${stringFor(timeZone)}"
  }

  def isEarlierInMonth(when: OffsetDateTime): Boolean = {
    when.getDayOfMonth < dayOfMonth || (when.getDayOfMonth == dayOfMonth && when.toLocalTime.isBefore(timeOfDay))
  }
  def isLaterInMonth(when: OffsetDateTime): Boolean = {
    when.getDayOfMonth > dayOfMonth || (when.getDayOfMonth == dayOfMonth && when.toLocalTime.isAfter(timeOfDay))
  }

  def withAdjustments(when: OffsetDateTime): OffsetDateTime = withStandardAdjustments(when.withDayOfMonth(dayOfMonth))

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

  val typeName = MonthlyByDayOfMonth.recurrenceType
  override val maybeDayOfMonth = Some(dayOfMonth)
  override val maybeTimeOfDay = Some(timeOfDay)

}

object MonthlyByDayOfMonth {
  val recurrenceType = "monthly_by_day_of_month"

  private def maybeDayOfMonthFrom(text: String): Option[Int] = {
    Recurrence.maybeOrdinalFor(text, Some(" month"))
  }

  def maybeFromText(text: String, defaultTimeZone: ZoneId): Option[MonthlyByDayOfMonth] = {
    if (Recurrence.includesDayOfWeek(text)) {
      return None
    }

    Recurrence.maybeMonthlyFrequencyFrom(text).map { frequency =>
      val maybeDayOfMonth = maybeDayOfMonthFrom(text)
      MonthlyByDayOfMonth(
        frequency,
        maybeDayOfMonth.getOrElse(OffsetDateTime.now.getDayOfMonth),
        Recurrence.ensureTimeFrom(text, defaultTimeZone),
        defaultTimeZone
      )
    }
  }
}

case class MonthlyByNthDayOfWeek(frequency: Int, dayOfWeek: DayOfWeek, nth: Int, timeOfDay: LocalTime, timeZone: ZoneId) extends RecurrenceWithTimeOfDay {

  override def displayString: String = {
    val frequencyString = if (frequency == 1) { "month" } else { s"$frequency months" }
    s"every $frequencyString on the ${Recurrence.ordinalStringFor(nth)} ${Recurrence.dayOfWeekNameFor(dayOfWeek)} at ${timeOfDay.format(Recurrence.timeFormatter)} ${stringFor(timeZone)}"
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

  def maybeFromText(text: String, defaultTimeZone: ZoneId): Option[MonthlyByNthDayOfWeek] = {
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

case class Yearly(frequency: Int, monthDay: MonthDay, timeOfDay: LocalTime, timeZone: ZoneId) extends RecurrenceWithTimeOfDay {

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

  val typeName = Yearly.recurrenceType
  override val maybeMonth = Some(month)
  override val maybeDayOfMonth = Some(dayOfMonth)
  override val maybeTimeOfDay = Some(timeOfDay)

}

object Yearly {
  val recurrenceType = "yearly"

  def maybeFromText(text: String, defaultTimeZone: ZoneId): Option[Yearly] = {

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

  val timeFormatter = DateTimeFormatter.ofPattern("h:mma")
  val timeFormatterWithZone = DateTimeFormatter.ofPattern("h:mma z")

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

  def maybeMonthDayFrom(text: String, defaultTimeZone: ZoneId): Option[MonthDay] = {
    val monthDayRegex = """(?i).*on\s+(.*?)(at.*)?$""".r
    text match {
      case monthDayRegex(monthDay, _) => maybeDateFrom(monthDay, defaultTimeZone).map { date =>
        val calendar = Calendar.getInstance(TimeZone.getTimeZone(defaultTimeZone))
        calendar.setTime(date)
        val javaMonth = calendar.get(Calendar.MONTH)
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        MonthDay.of(javaMonth + 1, dayOfMonth)
      }
      case _ => None
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
                 timeZone: ZoneId,
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
      case(Yearly.recurrenceType) => Yearly(frequency, MonthDay.of(maybeMonth.get, maybeDayOfMonth.get), maybeTimeOfDay.get, timeZone)
    }
  }

  def buildFor(raw: RawScheduledMessage, defaultTimeZone: ZoneId): Recurrence = {
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

  def maybeFromText(text: String, defaultTimeZone: ZoneId): Option[Recurrence] = {
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
