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
      recurrence.copyWithEmptyId mustBe otherRecurrence.copyWithEmptyId
    }
  }

  val justMonday = Seq(DayOfWeek.MONDAY)
  val justWednesday = Seq(DayOfWeek.WEDNESDAY)
  val mwf = Seq(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)

  "Minutely" should {

    "recur every minute" in {
      val recurrence = Minutely(IDs.next, 1)
      recurrence.nextAfter(dateTimeOf(2010, 6, 7, 9, 0, timeZone)) mustBe dateTimeOf(2010, 6, 7, 9, 1, timeZone)
    }

    "recur every 42 minutes" in {
      val recurrence = Minutely(IDs.next, 42)
      recurrence.nextAfter(dateTimeOf(2010, 6, 7, 9, 0, timeZone)) mustBe dateTimeOf(2010, 6, 7, 9, 42, timeZone)
    }

    "be created with implied frequency of 1" in {
      mustMatch(Recurrence.maybeUnsavedFromText("every minute", timeZone), Some(Minutely(IDs.next, 1)))
    }

    "be created with frequency" in {
      mustMatch(Recurrence.maybeUnsavedFromText("every 5 minutes", timeZone), Some(Minutely(IDs.next, 5)))
    }
  }

  "Hourly" should {

    "recur every 2h on the 42nd minute" in  {
      val recurrence = Hourly(IDs.next, 2, 42)
      recurrence.nextAfter(dateTimeOf(2010, 6, 7, 9, 42, timeZone)) mustBe dateTimeOf(2010, 6, 7, 11, 42, timeZone)
    }

    "recur later the same hour" in {
      val recurrence = Hourly(IDs.next, 1, 42)
      recurrence.nextAfter(dateTimeOf(2010, 6, 7, 9, 40, timeZone)) mustBe dateTimeOf(2010, 6, 7, 9, 42, timeZone)
    }

    "recur the next hour if past minute of hour" in {
      val recurrence = Hourly(IDs.next, 1, 42)
      recurrence.nextAfter(dateTimeOf(2010, 6, 7, 9, 43, timeZone)) mustBe dateTimeOf(2010, 6, 7, 10, 42, timeZone)
    }

    "have the right initial time when earlier in hour" in {
      val recurrence = Hourly(IDs.next, 2, 42)
      recurrence.initialAfter(dateTimeOf(2010, 6, 7, 9, 41, timeZone)) mustBe dateTimeOf(2010, 6, 7, 9, 42, timeZone)
    }

    "have the right initial time when later in hour" in {
      val recurrence = Hourly(IDs.next, 2, 42)
      recurrence.initialAfter(dateTimeOf(2010, 6, 7, 9, 43, timeZone)) mustBe dateTimeOf(2010, 6, 7, 10, 42, timeZone)
    }

    "have the right initial time when on same minute" in {
      val recurrence = Hourly(IDs.next, 2, 42)
      recurrence.initialAfter(dateTimeOf(2010, 6, 7, 9, 42, timeZone)) mustBe dateTimeOf(2010, 6, 7, 9, 42, timeZone)
    }

    "be created with implied frequency of 1" in {
      mustMatch(Recurrence.maybeUnsavedFromText("every hour", timeZone), Some(Hourly(IDs.next, 1, OffsetDateTime.now.getMinute)))
    }

    "be created with frequency" in {
      mustMatch(Recurrence.maybeUnsavedFromText("every 4 hours", timeZone), Some(Hourly(IDs.next, 4, OffsetDateTime.now.getMinute)))
    }

    "be created with frequency and minutes" in {
      mustMatch(Recurrence.maybeUnsavedFromText("every 4 hours at 15 minutes", timeZone), Some(Hourly(IDs.next, 4, 15)))
    }

  }

  "Daily" should {

    "recur every day at noon" in  {
      val recurrence = Daily(IDs.next, 1, LocalTime.parse("12:00:00"), timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 6, 7, 12, 0, timeZone)) mustBe dateTimeOf(2010, 6, 8, 12, 0, timeZone)
    }

    "recur later the same day" in  {
      val recurrence = Daily(IDs.next, 1, LocalTime.parse("12:00:00"), timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 6, 7, 11, 50, timeZone)) mustBe dateTimeOf(2010, 6, 7, 12, 0, timeZone)
    }

    "recur later the next day if already past the target time" in  {
      val recurrence = Daily(IDs.next, 1, LocalTime.parse("12:00:00"), timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 6, 7, 12, 50, timeZone)) mustBe dateTimeOf(2010, 6, 8, 12, 0, timeZone)
    }

    "have the right initial time when earlier in the day" in {
      val recurrence = Daily(IDs.next, 2, LocalTime.parse("12:00:00"), timeZone)
      recurrence.initialAfter(dateTimeOf(2010, 6, 7, 11, 59, timeZone)) mustBe dateTimeOf(2010, 6, 7, 12, 0, timeZone)
    }

    "have the right initial time when later in the day" in {
      val recurrence = Daily(IDs.next, 2, LocalTime.parse("12:00:00"), timeZone)
      recurrence.initialAfter(dateTimeOf(2010, 6, 7, 12, 1, timeZone)) mustBe dateTimeOf(2010, 6, 8, 12, 0, timeZone)
    }

    "have the right initial time when at the same point in the day" in {
      val recurrence = Daily(IDs.next, 2, LocalTime.parse("12:00:00"), timeZone)
      recurrence.initialAfter(dateTimeOf(2010, 6, 7, 12, 0, timeZone)) mustBe dateTimeOf(2010, 6, 7, 12, 0, timeZone)
    }

    "be created with implied frequency of 1" in {
      mustMatch(Recurrence.maybeUnsavedFromText("every day", timeZone), Some(Daily(IDs.next, 1, Recurrence.currentAdjustedTime(timeZone), timeZone)))
    }

    "be created with frequency" in {
      mustMatch(Recurrence.maybeUnsavedFromText("every 4 days", timeZone), Some(Daily(IDs.next, 4, Recurrence.currentAdjustedTime(timeZone), timeZone)))
    }

    "be created with frequency and time" in {
      mustMatch(Recurrence.maybeUnsavedFromText("every 4 days at 3pm", timeZone), Some(Daily(IDs.next, 4, LocalTime.parse("15:00"), timeZone)))
    }

    "use the timezone to determine the time, if specified, but save with default timezone" in {
      mustMatch(Recurrence.maybeUnsavedFromText("every 4 days at 3pm pacific", timeZone), Some(Daily(IDs.next, 4, LocalTime.parse("18:00"), timeZone)))
    }

    "use the default timezone if not specified in recurrence" in {
      val laTz = ZoneId.of("America/Los_Angeles")
      mustMatch(Recurrence.maybeUnsavedFromText("every 4 days at 3pm", laTz), Some(Daily(IDs.next, 4, LocalTime.parse("15:00"), laTz)))
    }

  }

  "Weekly" should {

    "recur every second week at Monday, 2pm" in  {
      val recurrence = Weekly(IDs.next, 2, justMonday, LocalTime.parse("14:00:00"), timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 6, 7, 14, 0, timeZone)) mustBe dateTimeOf(2010, 6, 21, 14, 0, timeZone)
    }

    "recur later in the week" in  {
      val recurrence = Weekly(IDs.next, 1, justWednesday, fivePM, timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 6, 8, 12, 1, timeZone)) mustBe dateTimeOf(2010, 6, 9, 17, 0, timeZone)
    }

    "recur the following week if already past target day" in  {
      val recurrence = Weekly(IDs.next, 1, justWednesday, fivePM, timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 6, 10, 12, 1, timeZone)) mustBe dateTimeOf(2010, 6, 16, 17, 0, timeZone)
    }

    "recur the next of multiple days in the week, if there is one" in {
      val recurrence = Weekly(IDs.next, 1, mwf, fivePM, timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 6, 8, 12, 1, timeZone)) mustBe dateTimeOf(2010, 6, 9, 17, 0, timeZone)
    }

    "recur the same day of multiple days in the week, if time is later in the day" in {
      val recurrence = Weekly(IDs.next, 1, mwf, fivePM, timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 6, 9, 12, 1, timeZone)) mustBe dateTimeOf(2010, 6, 9, 17, 0, timeZone)
    }

    "recur the following week if past all days in the week" in {
      val recurrence = Weekly(IDs.next, 1, mwf, fivePM, timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 6, 12, 12, 1, timeZone)) mustBe dateTimeOf(2010, 6, 14, 17, 0, timeZone)
    }

    "have the right initial time when earlier in the week" in {
      val recurrence = Weekly(IDs.next, 2, justWednesday, fivePM, timeZone)
      recurrence.initialAfter(dateTimeOf(2010, 6, 9, 16, 59, timeZone)) mustBe dateTimeOf(2010, 6, 9, 17, 0, timeZone)
    }

    "have the right initial time when later in the week" in {
      val recurrence = Weekly(IDs.next, 2, justWednesday, fivePM, timeZone)
      recurrence.initialAfter(dateTimeOf(2010, 6, 9, 17, 1, timeZone)) mustBe dateTimeOf(2010, 6, 16, 17, 0, timeZone)
    }

    "have the right initial time when at the same point in the week" in {
      val recurrence = Weekly(IDs.next, 2, justWednesday, fivePM, timeZone)
      recurrence.initialAfter(dateTimeOf(2010, 6, 9, 17, 0, timeZone)) mustBe dateTimeOf(2010, 6, 16, 17, 0, timeZone)
    }

    "be created with implied frequency of 1" in {
      mustMatch(Recurrence.maybeUnsavedFromText("every week", timeZone), Some(Weekly(IDs.next, 1, Seq(OffsetDateTime.now.getDayOfWeek), Recurrence.currentAdjustedTime(timeZone), timeZone)))
    }

    "be created with frequency" in {
      mustMatch(Recurrence.maybeUnsavedFromText("every 2 weeks", timeZone), Some(Weekly(IDs.next, 2, Seq(OffsetDateTime.now.getDayOfWeek), Recurrence.currentAdjustedTime(timeZone), timeZone)))
    }

    "be created with frequency, day of week and time" in {
      mustMatch(Recurrence.maybeUnsavedFromText("every 2 weeks on Monday at 3pm", timeZone), Some(Weekly(IDs.next, 2, justMonday, LocalTime.parse("15:00"), timeZone)))
    }

    "be created with frequency, multiple days of week and time" in {
      mustMatch(Recurrence.maybeUnsavedFromText("every 2 weeks on Monday Wednesday Friday at 3pm", timeZone),
        Some(Weekly(IDs.next, 2, mwf, LocalTime.parse("15:00"), timeZone)))
    }

  }

  "MonthlyByDayOfMonth" should {

    "recur the first of every second month, at 5pm" in  {
      val recurrence = MonthlyByDayOfMonth(IDs.next, 2, 1, fivePM, timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 6, 7, 12, 1, timeZone)) mustBe dateTimeOf(2010, 8, 1, 17, 0, timeZone)
    }

    "recur later in the month if starting from earlier day in the month" in  {
      val recurrence = MonthlyByDayOfMonth(IDs.next, 1, 6, fivePM, timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 6, 5, 12, 1, timeZone)) mustBe dateTimeOf(2010, 6, 6, 17, 0, timeZone)
    }

    "recur later in the day if starting from earlier time in target day" in  {
      val recurrence = MonthlyByDayOfMonth(IDs.next, 1, 6, fivePM, timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 6, 6, 12, 1, timeZone)) mustBe dateTimeOf(2010, 6, 6, 17, 0, timeZone)
    }

    "recur next month if starting later in the month" in  {
      val recurrence = MonthlyByDayOfMonth(IDs.next, 1, 6, fivePM, timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 6, 6, 17, 1, timeZone)) mustBe dateTimeOf(2010, 7, 6, 17, 0, timeZone)
    }

    "have the right initial time when earlier in the month" in {
      val recurrence = MonthlyByDayOfMonth(IDs.next, 1, 6, fivePM, timeZone)
      recurrence.initialAfter(dateTimeOf(2010, 6, 6, 16, 59, timeZone)) mustBe dateTimeOf(2010, 6, 6, 17, 0, timeZone)
    }

    "have the right initial time when later in the month" in {
      val recurrence = MonthlyByDayOfMonth(IDs.next, 1, 6, fivePM, timeZone)
      recurrence.initialAfter(dateTimeOf(2010, 6, 6, 17, 1, timeZone)) mustBe dateTimeOf(2010, 7, 6, 17, 0, timeZone)
    }

    "have the right initial time when at the same point in the month" in {
      val recurrence = MonthlyByDayOfMonth(IDs.next, 1, 6, fivePM, timeZone)
      recurrence.initialAfter(dateTimeOf(2010, 6, 6, 17, 0, timeZone)) mustBe dateTimeOf(2010, 6, 6, 17, 0, timeZone)
    }

    "be created with first day of every month" in {
      mustMatch(Recurrence.maybeUnsavedFromText("the first day of every month at 9am", timeZone), Some(MonthlyByDayOfMonth(IDs.next, 1, 1, LocalTime.parse("09:00"), timeZone)))
    }

    "be created with 15th day of every 3rd month" in {
      mustMatch(Recurrence.maybeUnsavedFromText("the 15th of every 3rd month at 5pm", timeZone), Some(MonthlyByDayOfMonth(IDs.next, 3, 15, fivePM, timeZone)))
    }

  }

  "MonthlyByNthDayOfWeek" should {

    "recur the second Tuesday of every month, at 5pm" in  {
      val recurrence = MonthlyByNthDayOfWeek(IDs.next, 1, DayOfWeek.TUESDAY, 2, fivePM, timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 6, 7, 12, 1, timeZone)) mustBe dateTimeOf(2010, 6, 8, 17, 0, timeZone)
    }

    "recur correctly when month starts with the target day of week" in  {
      val recurrence = MonthlyByNthDayOfWeek(IDs.next, 1, DayOfWeek.THURSDAY, 1, fivePM, timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 6, 7, 12, 1, timeZone)) mustBe dateTimeOf(2010, 7, 1, 17, 0, timeZone)
    }

    "recur correctly when month starts with a day before the target day of week" in  {
      val recurrence = MonthlyByNthDayOfWeek(IDs.next, 1, DayOfWeek.FRIDAY, 1, fivePM, timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 6, 7, 12, 1, timeZone)) mustBe dateTimeOf(2010, 7, 2, 17, 0, timeZone)
    }

    "recur correctly when month starts with a day after the target day of week" in  {
      val recurrence = MonthlyByNthDayOfWeek(IDs.next, 1, DayOfWeek.WEDNESDAY, 1, fivePM, timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 6, 7, 12, 1, timeZone)) mustBe dateTimeOf(2010, 7, 7, 17, 0, timeZone)
    }

    "recur later the same month if starting from earlier in the month" in  {
      val recurrence = MonthlyByNthDayOfWeek(IDs.next, 1, DayOfWeek.MONDAY, 1, fivePM, timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 6, 6, 12, 1, timeZone)) mustBe dateTimeOf(2010, 6, 7, 17, 0, timeZone)
    }

    "recur later the same month if starting from earlier in the target day" in  {
      val recurrence = MonthlyByNthDayOfWeek(IDs.next, 1, DayOfWeek.MONDAY, 1, fivePM, timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 6, 7, 12, 1, timeZone)) mustBe dateTimeOf(2010, 6, 7, 17, 0, timeZone)
    }

    "have the right initial time when earlier in the month" in {
      val recurrence = MonthlyByNthDayOfWeek(IDs.next, 1, DayOfWeek.MONDAY, 1, fivePM, timeZone)
      recurrence.initialAfter(dateTimeOf(2010, 6, 7, 16, 59, timeZone)) mustBe dateTimeOf(2010, 6, 7, 17, 0, timeZone)
    }

    "have the right initial time when later in the month" in {
      val recurrence = MonthlyByNthDayOfWeek(IDs.next, 1, DayOfWeek.MONDAY, 1, fivePM, timeZone)
      recurrence.initialAfter(dateTimeOf(2010, 6, 7, 17, 1, timeZone)) mustBe dateTimeOf(2010, 7, 5, 17, 0, timeZone)
    }

    "have the right initial time when at the same point in the month" in {
      val recurrence = MonthlyByNthDayOfWeek(IDs.next, 1, DayOfWeek.MONDAY, 1, fivePM, timeZone)
      recurrence.initialAfter(dateTimeOf(2010, 6, 7, 17, 0, timeZone)) mustBe dateTimeOf(2010, 6, 7, 17, 0, timeZone)
    }

    "be created with first monday of every month" in {
      mustMatch(Recurrence.maybeUnsavedFromText("the first monday of every month at 5pm", timeZone),
        Some(MonthlyByNthDayOfWeek(IDs.next, 1, DayOfWeek.MONDAY, 1, fivePM, timeZone)))
    }

    "be created with 2nd wednesday of every 3rd month" in {
      mustMatch(Recurrence.maybeUnsavedFromText("the 2nd wednesday of every 3rd month at 5pm", timeZone),
        Some(MonthlyByNthDayOfWeek(IDs.next, 3, DayOfWeek.WEDNESDAY, 2, fivePM, timeZone)))
    }

  }

  "Yearly" should {

    "recur Jan 14 every year, at 5pm" in  {
      val recurrence = Yearly(IDs.next, 1, MonthDay.of(1, 14), fivePM, timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 6, 7, 12, 1, timeZone)) mustBe dateTimeOf(2011, 1, 14, 17, 0, timeZone)
    }

    "recur later the same year" in  {
      val recurrence = Yearly(IDs.next, 1, MonthDay.of(1, 14), fivePM, timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 1, 13, 12, 1, timeZone)) mustBe dateTimeOf(2010, 1, 14, 17, 0, timeZone)
    }

    "recur later the same day" in  {
      val recurrence = Yearly(IDs.next, 1, MonthDay.of(1, 14), fivePM, timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 1, 14, 12, 1, timeZone)) mustBe dateTimeOf(2010, 1, 14, 17, 0, timeZone)
    }

    "recur the next year" in  {
      val recurrence = Yearly(IDs.next, 1, MonthDay.of(1, 14), fivePM, timeZone)
      recurrence.nextAfter(dateTimeOf(2010, 1, 14, 17, 1, timeZone)) mustBe dateTimeOf(2011, 1, 14, 17, 0, timeZone)
    }

    "have the right initial time when earlier in the year" in {
      val recurrence = Yearly(IDs.next, 1, MonthDay.of(1, 14), fivePM, timeZone)
      recurrence.initialAfter(dateTimeOf(2010, 1, 14, 16, 59, timeZone)) mustBe dateTimeOf(2010, 1, 14, 17, 0, timeZone)
    }

    "have the right initial time when later in the year" in {
      val recurrence = Yearly(IDs.next, 1, MonthDay.of(1, 14), fivePM, timeZone)
      recurrence.initialAfter(dateTimeOf(2010, 1, 14, 17, 1, timeZone)) mustBe dateTimeOf(2011, 1, 14, 17, 0, timeZone)
    }

    "have the right initial time when at the same point in the year" in {
      val recurrence = Yearly(IDs.next, 1, MonthDay.of(1, 14), fivePM, timeZone)
      recurrence.initialAfter(dateTimeOf(2010, 1, 14, 17, 0, timeZone)) mustBe dateTimeOf(2010, 1, 14, 17, 0, timeZone)
    }

    "be created for the 14th of January every year" in {
      mustMatch(Recurrence.maybeUnsavedFromText("every year on January 14 at 5pm", timeZone), Some(Yearly(IDs.next, 1, MonthDay.of(1, 14), fivePM, timeZone)))
    }

    "be created for every second January 14th with no time specified" in {
      val time = Recurrence.currentAdjustedTime(timeZone)
      mustMatch(Recurrence.maybeUnsavedFromText("every 2nd year on January 14", timeZone), Some(Yearly(IDs.next, 2, MonthDay.of(1, 14), time, timeZone)))
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



}
