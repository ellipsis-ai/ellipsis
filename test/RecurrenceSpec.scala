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

  "Minutely" should {

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
      Recurrence.maybeTimesToRunFromText("""schedule ":tada:" every minute once""") mustBe Some(1)
      Recurrence.maybeTimesToRunFromText("""schedule ":tada:" every year on December 31 at 4:56 once """) mustBe Some(1)
    }

    "return 2 for text that ends in twice" in {
      Recurrence.maybeTimesToRunFromText("""schedule ":tada:" every minute twice""") mustBe Some(2)
      Recurrence.maybeTimesToRunFromText("""schedule ":tada:" every year on December 31 at 4:56 twice """) mustBe Some(2)
    }

    "return a number for text that ends in N times" in {
      Recurrence.maybeTimesToRunFromText("""schedule ":tada:" every month on the 1st at 5pm, 4 times""") mustBe Some(4)
      Recurrence.maybeTimesToRunFromText("""schedule ":tada:" every week on Mondays at 12 1 time """) mustBe Some(1)
    }

    "return none for text that ends in 0 or a negative number times" in {
      Recurrence.maybeTimesToRunFromText("""schedule ":tada:" every 5 minutes 0 times """) mustBe None
      Recurrence.maybeTimesToRunFromText("""schedule ":tada:" every 5 minutes -560 times """) mustBe None
      Recurrence.maybeTimesToRunFromText("""schedule ":tada:" every month on the 31st at 9:00 -1 time""") mustBe None
    }

    "return none for text that doesn’t have any obvious N times" in {
      Recurrence.maybeTimesToRunFromText("""schedule ":tada:" every month on the 31st at the time 4:00""") mustBe None
      Recurrence.maybeTimesToRunFromText("""schedule ":tada:" once every month on the 1st at 9:50am""") mustBe None
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
