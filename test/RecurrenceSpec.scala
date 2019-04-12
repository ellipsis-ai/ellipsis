import java.time._

import models.IDs
import models.behaviors.scheduling.recurrence._
import org.scalatestplus.play.PlaySpec

class RecurrenceSpec extends PlaySpec {

  val fivePM = LocalTime.parse("17:00:00")

  val timeZone = ZoneId.of("America/Toronto")

  def dateTimeOf(year: Int, month: Int, day: Int, hour: Int, minute: Int, timeZone: ZoneId): OffsetDateTime = {
    ZonedDateTime.of(year, month, day, hour, minute, 0, 0, timeZone).toOffsetDateTime
  }

  def mustMatch(maybeRecurrence: Option[Recurrence], maybeOtherRecurrence: Option[Recurrence]) = {
    maybeRecurrence.isDefined mustBe maybeOtherRecurrence.isDefined
    for {
      recurrence <- maybeRecurrence
      otherRecurrence <- maybeOtherRecurrence
    } yield {
      recurrence.copyWithEmptyId mustEqual otherRecurrence.copyWithEmptyId
    }
  }

  val justMonday = Seq(DayOfWeek.MONDAY)
  val justWednesday = Seq(DayOfWeek.WEDNESDAY)
  val mwf = Seq(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)

  "Minutely" when {
    "recur every minute" in {
      val recurrence = Minutely(IDs.next, 1, 0, None)
      recurrence.nextAfter(dateTimeOf(2010, 6, 7, 9, 0, timeZone)) mustBe dateTimeOf(2010, 6, 7, 9, 1, timeZone)
    }

    "recur every 42 minutes" in {
      val recurrence = Minutely(IDs.next, 42, 0, None)
      recurrence.nextAfter(dateTimeOf(2010, 6, 7, 9, 0, timeZone)) mustBe dateTimeOf(2010, 6, 7, 9, 42, timeZone)
    }

    "be created with implied frequency of 1" in {
      mustMatch(Recurrence.maybeUnsavedFromText("every minute", timeZone), Some(Minutely(IDs.next, 1, 0, None)))
    }

    "be created with frequency" in {
      mustMatch(Recurrence.maybeUnsavedFromText("every 5 minutes", timeZone), Some(Minutely(IDs.next, 5, 0, None)))
    }

    "be created to run once when applicable" in {
      mustMatch(Recurrence.maybeUnsavedFromText("in 5 minutes", timeZone), Some(Minutely(IDs.next, 5, 0, Some(1))))
      mustMatch(Recurrence.maybeUnsavedFromText("in 1 minute", timeZone), Some(Minutely(IDs.next, 1, 0, Some(1))))
    }

    "be created to run N times when applicable" in {
      mustMatch(Recurrence.maybeUnsavedFromText("in 5 minutes, 5 times", timeZone), Some(Minutely(IDs.next, 5, 0, Some(5))))
      mustMatch(Recurrence.maybeUnsavedFromText("in 1 minute, 1 time", timeZone), Some(Minutely(IDs.next, 1, 0, Some(1))))
    }

    "couldRunAt always returns true because any timestamp satisfies Minutely parameters" in {
      val recurrence = Minutely(IDs.next, 1, 0, None)
      recurrence.couldRunAt(OffsetDateTime.now) mustBe true
      recurrence.couldRunAt(dateTimeOf(2019, 2, 1, 0, 0, ZoneId.of("America/St_Johns"))) mustBe true
      recurrence.couldRunAt(OffsetDateTime.now.plusMonths(1).plusMinutes(12).plusYears(5)) mustBe true
    }

    "expectedNextRunFor returns the provided timestamp if it is valid, in the future, and earlier than the second possible run" in {
      val now = OffsetDateTime.now
      val recurrence = Minutely(IDs.next, frequency = 5, timesHasRun = 0, maybeTotalTimesToRun = None)

      val plus3Minutes = now.plusMinutes(3)
      recurrence.expectedNextRunFor(now, Some(plus3Minutes)) mustBe plus3Minutes

      val plus4Minutes = now.plusMinutes(4)
      recurrence.expectedNextRunFor(now, Some(plus4Minutes)) mustBe plus4Minutes
    }

    "expectedNextRunFor returns the initial timestamp if it is invalid or later than the second possible run" in {
      val now = OffsetDateTime.now
      val recurrence = Minutely(IDs.next, frequency = 5, timesHasRun = 0, maybeTotalTimesToRun = None)
      val initial = recurrence.initialAfter(now)

      val plus7Minutes = now.plusMinutes(7)
      recurrence.expectedNextRunFor(now, Some(plus7Minutes)) mustBe initial

      val minus3Minutes = now.minusMinutes(3)
      recurrence.expectedNextRunFor(now, Some(minus3Minutes)) mustBe initial

      recurrence.expectedNextRunFor(now, None) mustBe initial
    }
  }

  "Hourly" should {

    "recur every 2h on the 42nd minute" in  {
      val recurrence = Hourly(IDs.next, 2, 0, None, 42)
      recurrence.nextAfter(dateTimeOf(2010, 6, 7, 9, 42, timeZone)) mustBe dateTimeOf(2010, 6, 7, 11, 42, timeZone)
    }

    "recur later the same hour" in {
      val recurrence = Hourly(IDs.next, 1, 0, None, 42)
      recurrence.nextAfter(dateTimeOf(2010, 6, 7, 9, 40, timeZone)) mustBe dateTimeOf(2010, 6, 7, 9, 42, timeZone)
    }

    "recur the next hour if past minute of hour" in {
      val recurrence = Hourly(IDs.next, 1, 0, None, 42)
      recurrence.nextAfter(dateTimeOf(2010, 6, 7, 9, 43, timeZone)) mustBe dateTimeOf(2010, 6, 7, 10, 42, timeZone)
    }

    "have the right initial time when earlier in hour" in {
      val recurrence = Hourly(IDs.next, 2, 0, None, 42)
      recurrence.initialAfter(dateTimeOf(2010, 6, 7, 9, 41, timeZone)) mustBe dateTimeOf(2010, 6, 7, 9, 42, timeZone)
    }

    "have the right initial time when later in hour" in {
      val recurrence = Hourly(IDs.next, 2, 0, None, 42)
      recurrence.initialAfter(dateTimeOf(2010, 6, 7, 9, 43, timeZone)) mustBe dateTimeOf(2010, 6, 7, 10, 42, timeZone)
    }

    "have the right initial time when on same minute" in {
      val recurrence = Hourly(IDs.next, 2, 0, None, 42)
      recurrence.initialAfter(dateTimeOf(2010, 6, 7, 9, 42, timeZone)) mustBe dateTimeOf(2010, 6, 7, 9, 42, timeZone)
    }

    "be created with implied frequency of 1" in {
      mustMatch(Recurrence.maybeUnsavedFromText("every hour", timeZone), Some(Hourly(IDs.next, 1, 0, None, OffsetDateTime.now.getMinute)))
    }

    "be created with frequency" in {
      mustMatch(Recurrence.maybeUnsavedFromText("every 4 hours", timeZone), Some(Hourly(IDs.next, 4, 0, None, OffsetDateTime.now.getMinute)))
    }

    "be created with frequency and minutes" in {
      mustMatch(Recurrence.maybeUnsavedFromText("every 4 hours at 15 minutes", timeZone), Some(Hourly(IDs.next, 4, 0, None, 15)))
    }

    "be created to run once when applicable" in {
      mustMatch(Recurrence.maybeUnsavedFromText("in 5 hours", timeZone), Some(Hourly(IDs.next, 5, 0, Some(1), OffsetDateTime.now.getMinute)))
      mustMatch(Recurrence.maybeUnsavedFromText("in 5 hours at 12 minutes", timeZone), Some(Hourly(IDs.next, 5, 0, Some(1), 12)))
      mustMatch(Recurrence.maybeUnsavedFromText("in 1 hour", timeZone), Some(Hourly(IDs.next, 1, 0, Some(1), OffsetDateTime.now.getMinute)))
      mustMatch(Recurrence.maybeUnsavedFromText("in 1 hour at 1 minute", timeZone), Some(Hourly(IDs.next, 1, 0, Some(1), 1)))
    }

    "be created to run N times when applicable" in {
      mustMatch(Recurrence.maybeUnsavedFromText("in 5 hours, 5 times", timeZone), Some(Hourly(IDs.next, 5, 0, Some(5), OffsetDateTime.now.getMinute)))
      mustMatch(Recurrence.maybeUnsavedFromText("in 5 hours at 12 minutes, 5 times", timeZone), Some(Hourly(IDs.next, 5, 0, Some(5), 12)))
      mustMatch(Recurrence.maybeUnsavedFromText("in 1 hour, 1 time", timeZone), Some(Hourly(IDs.next, 1, 0, Some(1), OffsetDateTime.now.getMinute)))
      mustMatch(Recurrence.maybeUnsavedFromText("in 1 hour at 1 minute, 1 time", timeZone), Some(Hourly(IDs.next, 1, 0, Some(1), 1)))
    }

    "couldRunAt returns true for any timestamp on the same minute of the hour" in {
      val recurrence = Hourly(IDs.next, 1, 0, None, minuteOfHour = 0)
      recurrence.couldRunAt(OffsetDateTime.now.withMinute(0)) mustBe true
      recurrence.couldRunAt(OffsetDateTime.now.withMinute(1)) mustBe false
      recurrence.couldRunAt(OffsetDateTime.now.withMinute(0).plusMonths(1).plusHours(4).plusYears(5)) mustBe true
    }

    "expectedNextRunFor returns the provided timestamp if it is valid, in the future, and earlier than the second possible run" in {
      val now = OffsetDateTime.now
      val recurrence = Hourly(IDs.next, frequency = 5, timesHasRun = 0, maybeTotalTimesToRun = None, minuteOfHour = 0)

      val plus3Hours = now.plusHours(3).withMinute(0)
      recurrence.expectedNextRunFor(now, Some(plus3Hours)) mustBe plus3Hours

      val plus4Hours = now.plusHours(4).withMinute(0)
      recurrence.expectedNextRunFor(now, Some(plus4Hours)) mustBe plus4Hours
    }

    "expectedNextRunFor returns the initial timestamp if it is invalid or later than the second possible run" in {
      val now = OffsetDateTime.now
      val recurrence = Hourly(IDs.next, frequency = 5, timesHasRun = 0, maybeTotalTimesToRun = None, minuteOfHour = 0)
      val initial = recurrence.initialAfter(now)

      val plus7Hours = now.plusHours(7).withMinute(0)
      recurrence.expectedNextRunFor(now, Some(plus7Hours)) mustBe initial

      val minus3Hours = now.minusHours(3).withMinute(0)
      recurrence.expectedNextRunFor(now, Some(minus3Hours)) mustBe initial

      val wrongMinute = now.plusHours(1).withMinute(1)
      recurrence.expectedNextRunFor(now, Some(wrongMinute)) mustBe initial

      recurrence.expectedNextRunFor(now, None) mustBe initial
    }
  }

  "Daily" should {

    "recur every day at noon" in  {
      val recurrence = Daily(IDs.next, 1, 0, None, LocalTime.parse("12:00:00"), timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 6, 7, 12, 0, timeZone)) mustBe dateTimeOf(2010, 6, 8, 12, 0, timeZone)
    }

    "recur later the same day" in  {
      val recurrence = Daily(IDs.next, 1, 0, None, LocalTime.parse("12:00:00"), timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 6, 7, 11, 50, timeZone)) mustBe dateTimeOf(2010, 6, 7, 12, 0, timeZone)
    }

    "recur later the next day if already past the target time" in  {
      val recurrence = Daily(IDs.next, 1, 0, None, LocalTime.parse("12:00:00"), timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 6, 7, 12, 50, timeZone)) mustBe dateTimeOf(2010, 6, 8, 12, 0, timeZone)
    }

    "have the right initial time when earlier in the day" in {
      val recurrence = Daily(IDs.next, 2, 0, None, LocalTime.parse("12:00:00"), timeZone)
      recurrence.initialAfter(dateTimeOf(2010, 6, 7, 11, 59, timeZone)) mustBe dateTimeOf(2010, 6, 7, 12, 0, timeZone)
    }

    "have the right initial time when later in the day" in {
      val recurrence = Daily(IDs.next, 2, 0, None, LocalTime.parse("12:00:00"), timeZone)
      recurrence.initialAfter(dateTimeOf(2010, 6, 7, 12, 1, timeZone)) mustBe dateTimeOf(2010, 6, 8, 12, 0, timeZone)
    }

    "have the right initial time when at the same point in the day" in {
      val recurrence = Daily(IDs.next, 2, 0, None, LocalTime.parse("12:00:00"), timeZone)
      recurrence.initialAfter(dateTimeOf(2010, 6, 7, 12, 0, timeZone)) mustBe dateTimeOf(2010, 6, 7, 12, 0, timeZone)
    }

    "be created with implied frequency of 1" in {
      mustMatch(Recurrence.maybeUnsavedFromText("every day", timeZone), Some(Daily(IDs.next, 1, 0, None, Recurrence.currentAdjustedTime(timeZone), timeZone)))
    }

    "be created with frequency" in {
      mustMatch(Recurrence.maybeUnsavedFromText("every 4 days", timeZone), Some(Daily(IDs.next, 4, 0, None, Recurrence.currentAdjustedTime(timeZone), timeZone)))
    }

    "be created with frequency and time" in {
      mustMatch(Recurrence.maybeUnsavedFromText("every 4 days at 3pm", timeZone), Some(Daily(IDs.next, 4, 0, None, LocalTime.parse("15:00"), timeZone)))
    }

    "use the timezone to determine the time, if specified, but save with default timezone" in {
      mustMatch(Recurrence.maybeUnsavedFromText("every 4 days at 3pm pacific", timeZone), Some(Daily(IDs.next, 4, 0, None, LocalTime.parse("18:00"), timeZone)))
    }

    "use the default timezone if not specified in recurrence" in {
      val laTz = ZoneId.of("America/Los_Angeles")
      mustMatch(Recurrence.maybeUnsavedFromText("every 4 days at 3pm", laTz), Some(Daily(IDs.next, 4, 0, None, LocalTime.parse("15:00"), laTz)))
    }

    "create a one-time recurrence for today or tomorrow" in {
      mustMatch(Recurrence.maybeUnsavedFromText("today at 00:00:00.0000", timeZone), None)
      mustMatch(Recurrence.maybeUnsavedFromText("today at 23:59:59.9999", timeZone), Some(Daily(IDs.next, 1, 0, Some(1), LocalTime.parse("23:59:59"), timeZone)))
      mustMatch(Recurrence.maybeUnsavedFromText("tomorrow at 00:00:00.0000", timeZone), Some(Daily(IDs.next, 1, 0, Some(1), LocalTime.parse("00:00:00"), timeZone)))
      mustMatch(Recurrence.maybeUnsavedFromText("tomorrow at 23:59:59.9999", timeZone), Some(Daily(IDs.next, 2, 0, Some(1), LocalTime.parse("23:59:59"), timeZone)))
    }

    "maybeNextInstanceForTodayOrTomorrow" should {
      "be none if a time before now for today is requested" in {
        Daily.maybeNextInstanceForTodayOrTomorrow("today", LocalTime.of(5, 0), LocalTime.of(6, 0)) mustBe None
      }
      "be 1 if a time after now for today is requested" in {
        Daily.maybeNextInstanceForTodayOrTomorrow("today", LocalTime.of(6, 0), LocalTime.of(5, 0)) mustBe Some(1)
      }
      "be none if it is neither today or tomorrow" in {
        Daily.maybeNextInstanceForTodayOrTomorrow("yesterday", LocalTime.of(6, 0), LocalTime.of(5, 0)) mustBe None
      }
      "be 1 if a time before now for tomorrow is requested" in {
        Daily.maybeNextInstanceForTodayOrTomorrow("tomorrow", LocalTime.of(5, 0), LocalTime.of(6, 0)) mustBe Some(1)
      }
      "be 2 if a time after now for tomorrow is requested" in {
        Daily.maybeNextInstanceForTodayOrTomorrow("tomorrow", LocalTime.of(7, 0), LocalTime.of(5, 0)) mustBe Some(2)
      }
    }

    "couldRunAt returns true for any timestamp with the correct time of day" in {
      val recurrence = Daily(IDs.next, 1, 0, None, timeOfDay = LocalTime.NOON, timeZone)
      val date = dateTimeOf(2019, 4, 12, 12, 0, timeZone)
      recurrence.couldRunAt(date) mustBe true
      recurrence.couldRunAt(date.withHour(13)) mustBe false
      recurrence.couldRunAt(date.plusYears(5)) mustBe true
      recurrence.couldRunAt(dateTimeOf(2019, 4, 12, 9, 0, ZoneId.of("America/Vancouver"))) mustBe true
      recurrence.couldRunAt(dateTimeOf(2019, 4, 12, 13, 30, ZoneId.of("America/St_Johns"))) mustBe true
      recurrence.couldRunAt(dateTimeOf(2019, 4, 12, 12, 0, ZoneId.of("Europe/London"))) mustBe false
      recurrence.couldRunAt(dateTimeOf(2019, 4, 12, 17, 0, ZoneId.of("Europe/London"))) mustBe true
    }

    "expectedNextRunFor returns the provided timestamp if it is valid, in the future, and earlier than the second possible run" in {
      val date = dateTimeOf(2019, 4, 1, 9, 0, timeZone)
      val recurrence = Daily(IDs.next, frequency = 5, timesHasRun = 0, maybeTotalTimesToRun = None, timeOfDay = LocalTime.NOON, timeZone)

      val plus3Days = date.plusDays(3).withHour(12).withMinute(0)
      recurrence.expectedNextRunFor(date, Some(plus3Days)) mustBe plus3Days

      val plus4Days = date.plusDays(4).withHour(12).withMinute(0)
      recurrence.expectedNextRunFor(date, Some(plus4Days)) mustBe plus4Days

      val nextDayUtc = dateTimeOf(2019, 4, 2, 16, 0, ZoneId.of("UTC"))
      recurrence.expectedNextRunFor(date, Some(nextDayUtc)) mustBe nextDayUtc
    }

    "expectedNextRunFor returns the initial timestamp if it is invalid or later than the second possible run" in {
      val now = OffsetDateTime.now.atZoneSameInstant(timeZone).toOffsetDateTime
      val recurrence = Daily(IDs.next, frequency = 5, timesHasRun = 0, maybeTotalTimesToRun = None, timeOfDay = LocalTime.NOON, timeZone)

      val initial = recurrence.initialAfter(now)

      val plus7Days = now.plusDays(7).withHour(12).withMinute(0)
      recurrence.expectedNextRunFor(now, Some(plus7Days)) mustBe initial

      val minus3Days = now.minusDays(3).withHour(12).withMinute(0)
      recurrence.expectedNextRunFor(now, Some(minus3Days)) mustBe initial

      val wrongTime = now.plusDays(1).withHour(23).withMinute(59)
      recurrence.expectedNextRunFor(now, Some(wrongTime)) mustBe initial

      recurrence.expectedNextRunFor(now, None) mustBe initial
    }

  }

  "Weekly" should {

    "recur every second week at Monday, 2pm" in  {
      val recurrence = Weekly(IDs.next, 2, 0, None, justMonday, LocalTime.parse("14:00:00"), timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 6, 7, 14, 0, timeZone)) mustBe dateTimeOf(2010, 6, 21, 14, 0, timeZone)
    }

    "recur later in the week" in  {
      val recurrence = Weekly(IDs.next, 1, 0, None, justWednesday, fivePM, timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 6, 8, 12, 1, timeZone)) mustBe dateTimeOf(2010, 6, 9, 17, 0, timeZone)
    }

    "recur the following week if already past target day" in  {
      val recurrence = Weekly(IDs.next, 1, 0, None, justWednesday, fivePM, timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 6, 10, 12, 1, timeZone)) mustBe dateTimeOf(2010, 6, 16, 17, 0, timeZone)
    }

    "recur the next of multiple days in the week, if there is one" in {
      val recurrence = Weekly(IDs.next, 1, 0, None, mwf, fivePM, timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 6, 8, 12, 1, timeZone)) mustBe dateTimeOf(2010, 6, 9, 17, 0, timeZone)
    }

    "recur the same day of multiple days in the week, if time is later in the day" in {
      val recurrence = Weekly(IDs.next, 1, 0, None, mwf, fivePM, timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 6, 9, 12, 1, timeZone)) mustBe dateTimeOf(2010, 6, 9, 17, 0, timeZone)
    }

    "recur the following week if past all days in the week" in {
      val recurrence = Weekly(IDs.next, 1, 0, None, mwf, fivePM, timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 6, 12, 12, 1, timeZone)) mustBe dateTimeOf(2010, 6, 14, 17, 0, timeZone)
    }

    "have the right initial time when earlier in the week" in {
      val recurrence = Weekly(IDs.next, 2, 0, None, justWednesday, fivePM, timeZone)
      recurrence.initialAfter(dateTimeOf(2010, 6, 9, 16, 59, timeZone)) mustBe dateTimeOf(2010, 6, 9, 17, 0, timeZone)
    }

    "have the right initial time when later in the week" in {
      val recurrence = Weekly(IDs.next, 2, 0, None, justWednesday, fivePM, timeZone)
      recurrence.initialAfter(dateTimeOf(2010, 6, 9, 17, 1, timeZone)) mustBe dateTimeOf(2010, 6, 16, 17, 0, timeZone)
    }

    "have the right initial time when at the same point in the week" in {
      val recurrence = Weekly(IDs.next, 2, 0, None, justWednesday, fivePM, timeZone)
      recurrence.initialAfter(dateTimeOf(2010, 6, 9, 17, 0, timeZone)) mustBe dateTimeOf(2010, 6, 16, 17, 0, timeZone)
    }

    "be created with implied frequency of 1" in {
      mustMatch(Recurrence.maybeUnsavedFromText("every week", timeZone), Some(Weekly(IDs.next, 1, 0, None, Seq(OffsetDateTime.now.getDayOfWeek), Recurrence.currentAdjustedTime(timeZone), timeZone)))
    }

    "be created with frequency" in {
      mustMatch(Recurrence.maybeUnsavedFromText("every 2 weeks", timeZone), Some(Weekly(IDs.next, 2, 0, None, Seq(OffsetDateTime.now.getDayOfWeek), Recurrence.currentAdjustedTime(timeZone), timeZone)))
    }

    "be created with frequency, day of week and time" in {
      mustMatch(Recurrence.maybeUnsavedFromText("every 2 weeks on Monday at 3pm", timeZone), Some(Weekly(IDs.next, 2, 0, None, justMonday, LocalTime.parse("15:00"), timeZone)))
    }

    "be created with frequency, multiple days of week and time" in {
      mustMatch(Recurrence.maybeUnsavedFromText("every 2 weeks on Monday Wednesday Friday at 3pm", timeZone),
        Some(Weekly(IDs.next, 2, 0, None, mwf, LocalTime.parse("15:00"), timeZone)))
    }

    """be created with implied weekly frequency for "every" days of the week""" in {
      mustMatch(Recurrence.maybeUnsavedFromText("""every Wednesday at 3pm""", timeZone),
        Some(Weekly(IDs.next, 1, 0, None, justWednesday, LocalTime.parse("15:00"), timeZone)))
      mustMatch(Recurrence.maybeUnsavedFromText("""every Monday, Wednesday and Friday at 3pm""", timeZone),
        Some(Weekly(IDs.next, 1, 0, None, mwf, LocalTime.parse("15:00"), timeZone)))
    }

    "create a one-time instance for single days of the week at a defined time" in {
      mustMatch(Recurrence.maybeUnsavedFromText("""on Monday at 12pm""", timeZone),
        Some(Weekly(IDs.next, 1, 0, Some(1), justMonday, LocalTime.parse("12:00"), timeZone)))
      mustMatch(Recurrence.maybeUnsavedFromText("""next Wednesday at 5pm""", timeZone),
        Some(Weekly(IDs.next, 1, 0, Some(1), justWednesday, LocalTime.parse("17:00"), timeZone)))
    }

    "create an N-time instance for multiple days of the week at a defined time" in {
      mustMatch(Recurrence.maybeUnsavedFromText("""on Monday, Wednesday and Friday at 9:30am""", timeZone),
        Some(Weekly(IDs.next, 1, 0, Some(3), mwf, LocalTime.parse("09:30"), timeZone)))
    }

    "couldRunAt returns true for any timestamp with the correct time of day and weekday" in {
      val date = dateTimeOf(2019, 4, 12, 12, 0, timeZone)
      val recurrence = Weekly(IDs.next, 1, 0, None, Seq(DayOfWeek.FRIDAY), timeOfDay = LocalTime.NOON, timeZone)
      recurrence.couldRunAt(date) mustBe true
      recurrence.couldRunAt(date.plusDays(1)) mustBe false
      recurrence.couldRunAt(date.withHour(13)) mustBe false
      recurrence.couldRunAt(date.plusWeeks(5)) mustBe true
      recurrence.couldRunAt(dateTimeOf(2019, 4, 12, 13, 30, ZoneId.of("America/St_Johns"))) mustBe true
    }

    "expectedNextRunFor returns the provided timestamp if it is valid, in the future, and earlier than the second possible run" in {
      val now = OffsetDateTime.now.atZoneSameInstant(timeZone).toOffsetDateTime
      val recurrence = Weekly(IDs.next, frequency = 5, timesHasRun = 0, maybeTotalTimesToRun = None, Seq(now.getDayOfWeek), timeOfDay = LocalTime.NOON, timeZone)

      val plus3Weeks = now.withHour(12).withMinute(0).plusWeeks(3)
      recurrence.expectedNextRunFor(now, Some(plus3Weeks)) mustBe plus3Weeks

      val plus4Weeks = now.withHour(12).withMinute(0).plusWeeks(4)
      recurrence.expectedNextRunFor(now, Some(plus4Weeks)) mustBe plus4Weeks
    }

    "expectedNextRunFor returns the initial timestamp if it is invalid or later than the second possible run" in {
      val now = OffsetDateTime.now.atZoneSameInstant(timeZone).toOffsetDateTime
      val recurrence = Weekly(IDs.next, frequency = 5, timesHasRun = 0, maybeTotalTimesToRun = None, Seq(now.getDayOfWeek), timeOfDay = LocalTime.NOON, timeZone)

      val initial = recurrence.initialAfter(now)

      val plus7Weeks = now.plusWeeks(7).withHour(12).withMinute(0)
      recurrence.expectedNextRunFor(now, Some(plus7Weeks)) mustBe initial

      val minus3Weeks = now.minusWeeks(3).withHour(12).withMinute(0)
      recurrence.expectedNextRunFor(now, Some(minus3Weeks)) mustBe initial

      val wrongTime = now.withHour(23).withMinute(59)
      recurrence.expectedNextRunFor(now, Some(wrongTime)) mustBe initial

      val wrongDay = now.plusDays(1).withHour(12).withMinute(0)
      recurrence.expectedNextRunFor(now, Some(wrongDay)) mustBe initial

      recurrence.expectedNextRunFor(now, None) mustBe initial
    }
  }

  "MonthlyByDayOfMonth" should {

    "recur the first of every second month, at 5pm" in  {
      val recurrence = MonthlyByDayOfMonth(IDs.next, 2, 0, None, 1, fivePM, timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 6, 7, 12, 1, timeZone)) mustBe dateTimeOf(2010, 8, 1, 17, 0, timeZone)
    }

    "recur later in the month if starting from earlier day in the month" in  {
      val recurrence = MonthlyByDayOfMonth(IDs.next, 1, 0, None, 6, fivePM, timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 6, 5, 12, 1, timeZone)) mustBe dateTimeOf(2010, 6, 6, 17, 0, timeZone)
    }

    "recur later in the day if starting from earlier time in target day" in  {
      val recurrence = MonthlyByDayOfMonth(IDs.next, 1, 0, None, 6, fivePM, timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 6, 6, 12, 1, timeZone)) mustBe dateTimeOf(2010, 6, 6, 17, 0, timeZone)
    }

    "recur next month if starting later in the month" in  {
      val recurrence = MonthlyByDayOfMonth(IDs.next, 1, 0, None, 6, fivePM, timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 6, 6, 17, 1, timeZone)) mustBe dateTimeOf(2010, 7, 6, 17, 0, timeZone)
    }

    "have the right initial time when earlier in the month" in {
      val recurrence = MonthlyByDayOfMonth(IDs.next, 1, 0, None, 6, fivePM, timeZone)
      recurrence.initialAfter(dateTimeOf(2010, 6, 6, 16, 59, timeZone)) mustBe dateTimeOf(2010, 6, 6, 17, 0, timeZone)
    }

    "have the right initial time when later in the month" in {
      val recurrence = MonthlyByDayOfMonth(IDs.next, 1, 0, None, 6, fivePM, timeZone)
      recurrence.initialAfter(dateTimeOf(2010, 6, 6, 17, 1, timeZone)) mustBe dateTimeOf(2010, 7, 6, 17, 0, timeZone)
    }

    "have the right initial time when at the same point in the month" in {
      val recurrence = MonthlyByDayOfMonth(IDs.next, 1, 0, None, 6, fivePM, timeZone)
      recurrence.initialAfter(dateTimeOf(2010, 6, 6, 17, 0, timeZone)) mustBe dateTimeOf(2010, 6, 6, 17, 0, timeZone)
    }

    "use the last day of the month when the day number is higher than the last day of the month" in {
      val recurrence = MonthlyByDayOfMonth(IDs.next, 1, 0, None, 31, fivePM, timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 1, 31, 17, 1, timeZone)) mustBe dateTimeOf(2010, 2, 28, 17, 0, timeZone)
    }

    "use the last day of the month in leap years when the day number is higher than the last day of the month" in {
      val recurrence = MonthlyByDayOfMonth(IDs.next, 1, 0, None, 31, fivePM, timeZone)
      recurrence.nextAfter(dateTimeOf(2016, 1, 31, 17, 1, timeZone)) mustBe dateTimeOf(2016, 2, 29, 17, 0, timeZone)
    }

    "use the last day of the month when the day number is higher than the last day of the month, and it is before the time requested on the last day of the month" in {
      val recurrence = MonthlyByDayOfMonth(IDs.next, 1, 0, None, 31, fivePM, timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 2, 28, 12, 1, timeZone)) mustBe dateTimeOf(2010, 2, 28, 17, 0, timeZone)
    }

    "use the last day of next month when the day number is higher than the last day of the month, and it is past the time requested on the last day of the month" in {
      val recurrence = MonthlyByDayOfMonth(IDs.next, 1, 0, None, 31, fivePM, timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 2, 28, 18, 1, timeZone)) mustBe dateTimeOf(2010, 3, 31, 17, 0, timeZone)
    }

    "have the initial time be the last day of the month when the day number is higher than the last day of the month" in {
      val recurrence = MonthlyByDayOfMonth(IDs.next, 1, 0, None, 31, fivePM, timeZone)
      recurrence.initialAfter(dateTimeOf(2010, 2, 1, 12, 1, timeZone)) mustBe dateTimeOf(2010, 2, 28, 17, 0, timeZone)
    }

    "have the initial time be the last day of this month when the day number is higher than the last day of this month, and it is before the time requested on the last day of this month" in {
      val recurrence = MonthlyByDayOfMonth(IDs.next, 1, 0, None, 31, fivePM, timeZone)
      recurrence.initialAfter(dateTimeOf(2010, 2, 28, 12, 1, timeZone)) mustBe dateTimeOf(2010, 2, 28, 17, 0, timeZone)
    }

    "have the initial time be the appropriate day of the next month when the day number is higher than the last day of this month, and it is past the time requested on the last day of this month" in {
      val recurrence = MonthlyByDayOfMonth(IDs.next, 1, 0, None, 31, fivePM, timeZone)
      recurrence.initialAfter(dateTimeOf(2010, 2, 28, 18, 1, timeZone)) mustBe dateTimeOf(2010, 3, 31, 17, 0, timeZone)
    }

    "be created with first day of every month" in {
      mustMatch(Recurrence.maybeUnsavedFromText("the first day of every month at 9am", timeZone), Some(MonthlyByDayOfMonth(IDs.next, 1, 0, None, 1, LocalTime.parse("09:00"), timeZone)))
    }

    "be created with 15th day of every 3rd month" in {
      mustMatch(Recurrence.maybeUnsavedFromText("the 15th of every 3rd month at 5pm", timeZone), Some(MonthlyByDayOfMonth(IDs.next, 3, 0, None, 15, fivePM, timeZone)))
    }

    "couldRunAt returns true for any timestamp with the correct time of day and day of month" in {
      val date = dateTimeOf(2019, 4, 1, 12, 0, timeZone)
      val recurrence = MonthlyByDayOfMonth(IDs.next, 1, 0, None, dayOfMonth = 1, timeOfDay = LocalTime.NOON, timeZone)
      recurrence.couldRunAt(date) mustBe true
      recurrence.couldRunAt(date.withDayOfMonth(2)) mustBe false
      recurrence.couldRunAt(date.withHour(13)) mustBe false
      recurrence.couldRunAt(date.plusYears(5).plusMonths(5)) mustBe true
      recurrence.couldRunAt(dateTimeOf(2019, 4, 1, 13, 30, ZoneId.of("America/St_Johns"))) mustBe true
      recurrence.couldRunAt(dateTimeOf(2019, 4, 2, 1, 0, ZoneId.of("Asia/Tokyo"))) mustBe true
    }

    "expectedNextRunFor returns the provided timestamp if it is valid, in the future, and earlier than the second possible run" in {
      val now = OffsetDateTime.now.atZoneSameInstant(timeZone).toOffsetDateTime
      val recurrence = MonthlyByDayOfMonth(IDs.next, frequency = 5, timesHasRun = 0, maybeTotalTimesToRun = None, dayOfMonth = 1, timeOfDay = LocalTime.NOON, timeZone)

      val plus3Months = now.withDayOfMonth(1).plusMonths(3).withHour(12).withMinute(0)
      recurrence.expectedNextRunFor(now, Some(plus3Months)) mustBe plus3Months

      val plus4Months = now.withDayOfMonth(1).plusMonths(4).withHour(12).withMinute(0)
      recurrence.expectedNextRunFor(now, Some(plus4Months)) mustBe plus4Months
    }

    "expectedNextRunFor returns the initial timestamp if it is invalid or later than the second possible run" in {
      val now = OffsetDateTime.now.atZoneSameInstant(timeZone).toOffsetDateTime
      val recurrence = MonthlyByDayOfMonth(IDs.next, frequency = 5, timesHasRun = 0, maybeTotalTimesToRun = None, dayOfMonth = 1, timeOfDay = LocalTime.NOON, timeZone)

      val initial = recurrence.initialAfter(now)

      val plus7Months = now.withDayOfMonth(1).plusMonths(7).withHour(12).withMinute(0)
      recurrence.expectedNextRunFor(now, Some(plus7Months)) mustBe initial

      val minus3Months = now.withDayOfMonth(1).minusMonths(3).withHour(12).withMinute(0)
      recurrence.expectedNextRunFor(now, Some(minus3Months)) mustBe initial

      val wrongTime = now.withDayOfMonth(1).plusMonths(1).withHour(23).withMinute(59)
      recurrence.expectedNextRunFor(now, Some(wrongTime)) mustBe initial

      val wrongDay = now.withDayOfMonth(2).plusMonths(1).withHour(12).withMinute(0)
      recurrence.expectedNextRunFor(now, Some(wrongDay)) mustBe initial

      recurrence.expectedNextRunFor(now, None) mustBe initial
    }
  }

  "MonthlyByNthDayOfWeek" should {

    "recur the second Tuesday of every month, at 5pm" in  {
      val recurrence = MonthlyByNthDayOfWeek(IDs.next, 1, 0, None, DayOfWeek.TUESDAY, 2, fivePM, timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 6, 7, 12, 1, timeZone)) mustBe dateTimeOf(2010, 6, 8, 17, 0, timeZone)
    }

    "recur correctly when month starts with the target day of week" in  {
      val recurrence = MonthlyByNthDayOfWeek(IDs.next, 1, 0, None, DayOfWeek.THURSDAY, 1, fivePM, timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 6, 7, 12, 1, timeZone)) mustBe dateTimeOf(2010, 7, 1, 17, 0, timeZone)
    }

    "recur correctly when month starts with a day before the target day of week" in  {
      val recurrence = MonthlyByNthDayOfWeek(IDs.next, 1, 0, None, DayOfWeek.FRIDAY, 1, fivePM, timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 6, 7, 12, 1, timeZone)) mustBe dateTimeOf(2010, 7, 2, 17, 0, timeZone)
    }

    "recur correctly when month starts with a day after the target day of week" in  {
      val recurrence = MonthlyByNthDayOfWeek(IDs.next, 1, 0, None, DayOfWeek.WEDNESDAY, 1, fivePM, timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 6, 7, 12, 1, timeZone)) mustBe dateTimeOf(2010, 7, 7, 17, 0, timeZone)
    }

    "recur later the same month if starting from earlier in the month" in  {
      val recurrence = MonthlyByNthDayOfWeek(IDs.next, 1, 0, None, DayOfWeek.MONDAY, 1, fivePM, timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 6, 6, 12, 1, timeZone)) mustBe dateTimeOf(2010, 6, 7, 17, 0, timeZone)
    }

    "recur later the same month if starting from earlier in the target day" in  {
      val recurrence = MonthlyByNthDayOfWeek(IDs.next, 1, 0, None, DayOfWeek.MONDAY, 1, fivePM, timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 6, 7, 12, 1, timeZone)) mustBe dateTimeOf(2010, 6, 7, 17, 0, timeZone)
    }

    "have the right initial time when earlier in the month" in {
      val recurrence = MonthlyByNthDayOfWeek(IDs.next, 1, 0, None, DayOfWeek.MONDAY, 1, fivePM, timeZone)
      recurrence.initialAfter(dateTimeOf(2010, 6, 7, 16, 59, timeZone)) mustBe dateTimeOf(2010, 6, 7, 17, 0, timeZone)
    }

    "have the right initial time when later in the month" in {
      val recurrence = MonthlyByNthDayOfWeek(IDs.next, 1, 0, None, DayOfWeek.MONDAY, 1, fivePM, timeZone)
      recurrence.initialAfter(dateTimeOf(2010, 6, 7, 17, 1, timeZone)) mustBe dateTimeOf(2010, 7, 5, 17, 0, timeZone)
    }

    "have the right initial time when at the same point in the month" in {
      val recurrence = MonthlyByNthDayOfWeek(IDs.next, 1, 0, None, DayOfWeek.MONDAY, 1, fivePM, timeZone)
      recurrence.initialAfter(dateTimeOf(2010, 6, 7, 17, 0, timeZone)) mustBe dateTimeOf(2010, 6, 7, 17, 0, timeZone)
    }

    "be created with first monday of every month" in {
      mustMatch(Recurrence.maybeUnsavedFromText("the first monday of every month at 5pm", timeZone),
        Some(MonthlyByNthDayOfWeek(IDs.next, 1, 0, None, DayOfWeek.MONDAY, 1, fivePM, timeZone)))
    }

    "be created with 2nd wednesday of every 3rd month" in {
      mustMatch(Recurrence.maybeUnsavedFromText("the 2nd wednesday of every 3rd month at 5pm", timeZone),
        Some(MonthlyByNthDayOfWeek(IDs.next, 3, 0, None, DayOfWeek.WEDNESDAY, 2, fivePM, timeZone)))
    }

    "couldRunAt returns true for any timestamp with the correct time of day, day of week, and occurrence in the month" in {
      val recurrence = MonthlyByNthDayOfWeek(IDs.next, 1, 0, None, dayOfWeek = DayOfWeek.MONDAY, nth = 1, timeOfDay = LocalTime.NOON, timeZone)

      recurrence.couldRunAt(dateTimeOf(2018, 4, 1,12, 0, timeZone)) mustBe false
      recurrence.couldRunAt(dateTimeOf(2018, 4, 2,12, 0, timeZone)) mustBe true
      recurrence.couldRunAt(dateTimeOf(2019, 4, 1,12, 0, timeZone)) mustBe true
      recurrence.couldRunAt(dateTimeOf(2019, 4, 2,12, 0, timeZone)) mustBe false
      recurrence.couldRunAt(dateTimeOf(2019, 4, 1,13, 0, timeZone)) mustBe false
      recurrence.couldRunAt(dateTimeOf(2019, 4, 8,12, 0, timeZone)) mustBe false
      recurrence.couldRunAt(dateTimeOf(2019, 5, 1,12, 0, timeZone)) mustBe false
      recurrence.couldRunAt(dateTimeOf(2019, 5, 6,12, 0, timeZone)) mustBe true
      recurrence.couldRunAt(dateTimeOf(2020, 4, 1,12, 0, timeZone)) mustBe false
      recurrence.couldRunAt(dateTimeOf(2019, 4, 1, 13, 30, ZoneId.of("America/St_Johns"))) mustBe true
    }

    "expectedNextRunFor returns the provided timestamp if it is valid, in the future, and earlier than the second possible run" in {
      val start = dateTimeOf(2019, 4, 1, 0, 0, timeZone)
      val recurrence = MonthlyByNthDayOfWeek(IDs.next, frequency = 5, timesHasRun = 0, maybeTotalTimesToRun = None, dayOfWeek = DayOfWeek.MONDAY, nth = 1, timeOfDay = LocalTime.NOON, timeZone)

      val nextMonth = dateTimeOf(2019, 5, 6,12, 0, timeZone)
      recurrence.expectedNextRunFor(start, Some(nextMonth)) mustBe nextMonth

      val plus4Months = dateTimeOf(2019, 8, 5, 12, 0, timeZone)
      recurrence.expectedNextRunFor(start, Some(plus4Months)) mustBe plus4Months
    }

    "expectedNextRunFor returns the initial timestamp if it is invalid or later than the second possible run" in {
      val start = dateTimeOf(2019, 4, 1, 0, 0, timeZone)
      val recurrence = MonthlyByNthDayOfWeek(IDs.next, frequency = 5, timesHasRun = 0, maybeTotalTimesToRun = None, dayOfWeek = DayOfWeek.MONDAY, nth = 1, timeOfDay = LocalTime.NOON, timeZone)

      val initial = recurrence.initialAfter(start)

      val plus6Months = dateTimeOf(2019, 10, 7, 12, 0, timeZone)
      recurrence.expectedNextRunFor(start, Some(plus6Months)) mustBe initial

      val minus3Months = dateTimeOf(2019, 1, 7, 12, 0, timeZone)
      recurrence.expectedNextRunFor(start, Some(minus3Months)) mustBe initial

      val wrongTime = dateTimeOf(2019, 4, 1, 11, 0, timeZone)
      recurrence.expectedNextRunFor(start, Some(wrongTime)) mustBe initial

      val wrongDay = dateTimeOf(2019, 4, 8, 12, 0, timeZone)
      recurrence.expectedNextRunFor(start, Some(wrongDay)) mustBe initial

      recurrence.expectedNextRunFor(start, None) mustBe initial
    }
  }

  "Yearly" should {

    "recur Jan 14 every year, at 5pm" in  {
      val recurrence = Yearly(IDs.next, 1, 0, None, MonthDay.of(1, 14), fivePM, timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 6, 7, 12, 1, timeZone)) mustBe dateTimeOf(2011, 1, 14, 17, 0, timeZone)
    }

    "recur later the same year" in  {
      val recurrence = Yearly(IDs.next, 1, 0, None, MonthDay.of(1, 14), fivePM, timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 1, 13, 12, 1, timeZone)) mustBe dateTimeOf(2010, 1, 14, 17, 0, timeZone)
    }

    "recur later the same day" in  {
      val recurrence = Yearly(IDs.next, 1, 0, None, MonthDay.of(1, 14), fivePM, timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 1, 14, 12, 1, timeZone)) mustBe dateTimeOf(2010, 1, 14, 17, 0, timeZone)
    }

    "recur the next year" in  {
      val recurrence = Yearly(IDs.next, 1, 0, None, MonthDay.of(1, 14), fivePM, timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 1, 14, 17, 1, timeZone)) mustBe dateTimeOf(2011, 1, 14, 17, 0, timeZone)
    }

    "have the right initial time when earlier in the year" in {
      val recurrence = Yearly(IDs.next, 1, 0, None, MonthDay.of(1, 14), fivePM, timeZone)
      recurrence.initialAfter(dateTimeOf(2010, 1, 14, 16, 59, timeZone)) mustBe dateTimeOf(2010, 1, 14, 17, 0, timeZone)
    }

    "have the right initial time when later in the year" in {
      val recurrence = Yearly(IDs.next, 1, 0, None, MonthDay.of(1, 14), fivePM, timeZone)
      recurrence.initialAfter(dateTimeOf(2010, 1, 14, 17, 1, timeZone)) mustBe dateTimeOf(2011, 1, 14, 17, 0, timeZone)
    }

    "have the right initial time when at the same point in the year" in {
      val recurrence = Yearly(IDs.next, 1, 0, None, MonthDay.of(1, 14), fivePM, timeZone)
      recurrence.initialAfter(dateTimeOf(2010, 1, 14, 17, 0, timeZone)) mustBe dateTimeOf(2010, 1, 14, 17, 0, timeZone)
    }

    "be created for the 14th of January every year" in {
      mustMatch(Recurrence.maybeUnsavedFromText("every year on January 14 at 5pm", timeZone), Some(Yearly(IDs.next, 1, 0, None, MonthDay.of(1, 14), fivePM, timeZone)))
    }

    "be created for every second January 14th with no time specified" in {
      val time = Recurrence.currentAdjustedTime(timeZone)
      mustMatch(Recurrence.maybeUnsavedFromText("every 2nd year on January 14", timeZone), Some(Yearly(IDs.next, 2, 0, None, MonthDay.of(1, 14), time, timeZone)))
    }

    "create a one-time recurrence for a single date/time" in {
      mustMatch(Recurrence.maybeUnsavedFromText("""on January 1 at 12pm""", timeZone),
        Some(Yearly(IDs.next, 1, 0, Some(1), MonthDay.of(1, 1), LocalTime.parse("12:00"), timeZone)))

    }

    "create a one-time recurrence for a future year's date/time" in {
      val now = LocalDateTime.now(timeZone)
      val nextYear = now.getYear + 1

      mustMatch(Recurrence.maybeUnsavedFromText(s"""on January 1, ${nextYear.toString} at 12am""", timeZone),
        Some(Yearly(IDs.next, 1, 0, Some(1), MonthDay.of(1, 1), LocalTime.parse("00:00"), timeZone)))

      val tomorrowMidnight = now.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0)
      val fiveYearsLater = tomorrowMidnight.plusYears(5)
      val formatted = fiveYearsLater.format(Recurrence.dateFormatter)
      val desiredMonthDay = MonthDay.of(fiveYearsLater.getMonth, fiveYearsLater.getDayOfMonth)
      mustMatch(Recurrence.maybeUnsavedFromText(s"""on ${formatted} at 9am""", timeZone),
        Some(Yearly(IDs.next, 6, 0, Some(1), desiredMonthDay, LocalTime.parse("09:00"), timeZone)))
    }

    "not create a one-time recurrence for a past year's date/time" in {
      val now = LocalDateTime.now(timeZone)
      val lastYearToday = now.minusYears(1)
      val formatted = lastYearToday.format(Recurrence.dateFormatter)
      mustMatch(Recurrence.maybeUnsavedFromText(s"""on ${formatted} at 9am""", timeZone), None)
    }

    // TODO: The date parser doesn't differentiate between dates with and without a year, so there's no
    // good way to ignore dates in the past but in the current year
    "not create a one-time recurrence for this year in the past" ignore {
      val now = LocalDateTime.now(timeZone)
      val thisYear = now.getYear
      mustMatch(Recurrence.maybeUnsavedFromText(s"""on January 1, ${thisYear.toString} at 12am""", timeZone),
        None)
    }

    "couldRunAt returns true for any timestamp with the correct time of day, day of week, and occurrence in the month" in {
      val recurrence = Yearly(IDs.next, 1, 0, None, MonthDay.of(7, 28), timeOfDay = LocalTime.NOON, timeZone)

      recurrence.couldRunAt(dateTimeOf(2018, 7, 28,12, 0, timeZone)) mustBe true
      recurrence.couldRunAt(dateTimeOf(2019, 7, 28,12, 0, timeZone)) mustBe true
      recurrence.couldRunAt(dateTimeOf(2020, 7, 28,12, 0, timeZone)) mustBe true
      recurrence.couldRunAt(dateTimeOf(2019, 7, 27,12, 0, timeZone)) mustBe false
      recurrence.couldRunAt(dateTimeOf(2019, 8, 27,12, 0, timeZone)) mustBe false
      recurrence.couldRunAt(dateTimeOf(2019, 7, 31,13, 0, timeZone)) mustBe false
      recurrence.couldRunAt(dateTimeOf(2019, 7, 28,13, 30, ZoneId.of("America/St_Johns"))) mustBe true
    }

    "expectedNextRunFor returns the provided timestamp if it is valid, in the future, and earlier than the second possible run" in {
      val start = dateTimeOf(2019, 4, 1, 0, 0, timeZone)
      val recurrence = Yearly(IDs.next, 5, 0, None, MonthDay.of(7, 28), timeOfDay = LocalTime.NOON, timeZone)

      val nextYear = dateTimeOf(2020, 7, 28,12, 0, timeZone)
      recurrence.expectedNextRunFor(start, Some(nextYear)) mustBe nextYear

      val plus4Years = dateTimeOf(2023, 7, 28, 12, 0, timeZone)
      recurrence.expectedNextRunFor(start, Some(plus4Years)) mustBe plus4Years
    }

    "expectedNextRunFor returns the initial timestamp if it is invalid or later than the second possible run" in {
      val start = dateTimeOf(2019, 4, 1, 0, 0, timeZone)
      val recurrence = Yearly(IDs.next, 5, 0, None, MonthDay.of(7, 28), timeOfDay = LocalTime.NOON, timeZone)

      val initial = recurrence.initialAfter(start)

      val plus6Years = dateTimeOf(2025, 7, 28, 12, 0, timeZone)
      recurrence.expectedNextRunFor(start, Some(plus6Years)) mustBe initial

      val minus3Years = dateTimeOf(2016, 7, 28, 12, 0, timeZone)
      recurrence.expectedNextRunFor(start, Some(minus3Years)) mustBe initial

      val wrongTime = dateTimeOf(2019, 7, 28, 11, 0, timeZone)
      recurrence.expectedNextRunFor(start, Some(wrongTime)) mustBe initial

      val wrongDay = dateTimeOf(2019, 7, 29, 12, 0, timeZone)
      recurrence.expectedNextRunFor(start, Some(wrongDay)) mustBe initial

      val wrongMonth = dateTimeOf(2019, 8, 28, 12, 0, timeZone)
      recurrence.expectedNextRunFor(start, Some(wrongMonth)) mustBe initial

      recurrence.expectedNextRunFor(start, None) mustBe initial
    }
  }

  "Recurrence.daysOfWeekFrom" should {

    val monday = DayOfWeek.MONDAY
    val tuesday = DayOfWeek.TUESDAY
    val wednesday = DayOfWeek.WEDNESDAY
    val thursday = DayOfWeek.THURSDAY
    val friday = DayOfWeek.FRIDAY

    "handle space separated" in {
      Recurrence.daysOfWeekFrom("Monday Wednesday Friday") mustBe Seq(monday, wednesday, friday)
    }

    "handle comma separated" in {
      Recurrence.daysOfWeekFrom("Monday, Wednesday, Friday") mustBe Seq(monday, wednesday, friday)
    }

    "not care about capitalization" in {
      Recurrence.daysOfWeekFrom("monDAY, wedNESday, friday") mustBe Seq(monday, wednesday, friday)
    }

    "work for abbreviations" in {
      Recurrence.daysOfWeekFrom("mon, wed, fri") mustBe Seq(monday, wednesday, friday)
    }

    "handle weekdays" in {
      Recurrence.daysOfWeekFrom("every weekday") mustBe Seq(monday, tuesday, wednesday, thursday, friday)
    }
  }

  "Recurrence.maybeTimesToRunFromText" should {
    "return 1 for text that ends in once" in {
      Recurrence.maybeTimesToRunFromText("""every minute once""") mustBe Some(1)
      Recurrence.maybeTimesToRunFromText("""every year on December 31 at 4:56 once """) mustBe Some(1)
    }

    "return 2 for text that ends in twice" in {
      Recurrence.maybeTimesToRunFromText("""every minute twice""") mustBe Some(2)
      Recurrence.maybeTimesToRunFromText("""every year on December 31 at 4:56 twice """) mustBe Some(2)
    }

    "return a number for text that ends in N times" in {
      Recurrence.maybeTimesToRunFromText("""every month on the 1st at 5pm, 4 times""") mustBe Some(4)
      Recurrence.maybeTimesToRunFromText("""every week on Mondays at 12 1 time """) mustBe Some(1)
    }

    "return none for text that ends in 0 or a negative number times" in {
      Recurrence.maybeTimesToRunFromText("""every 5 minutes 0 times """) mustBe None
      Recurrence.maybeTimesToRunFromText("""every 5 minutes -560 times """) mustBe None
      Recurrence.maybeTimesToRunFromText("""every month on the 31st at 9:00 -1 time""") mustBe None
    }

    "return none for text that doesn’t have any obvious N times" in {
      Recurrence.maybeTimesToRunFromText("""every month on the 31st at the time 4:00""") mustBe None
      Recurrence.maybeTimesToRunFromText("""once every month on the 1st at 9:50am""") mustBe None
    }
  }

  "Any Recurrence shouldRunAgainAfterNextRun" should {
    "return true if there is no times to run set" in {
      Minutely(IDs.next, 1, 0, None).shouldRunAgainAfterNextRun mustBe true
    }

    "return true if there is at least one more run after the next" in {
      Minutely(IDs.next, 1, 0, Some(2)).shouldRunAgainAfterNextRun mustBe true
      Minutely(IDs.next, 1, 5, Some(10)).shouldRunAgainAfterNextRun mustBe true
    }

    "return false if there is only one more run or less" in {
      Minutely(IDs.next, 1, 0, Some(1)).shouldRunAgainAfterNextRun mustBe false
      Minutely(IDs.next, 1, 99, Some(100)).shouldRunAgainAfterNextRun mustBe false
      Minutely(IDs.next, 1, 100, Some(100)).shouldRunAgainAfterNextRun mustBe false
    }
  }



}
