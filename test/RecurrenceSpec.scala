import java.time.DayOfWeek

import models.behaviors.scheduledmessage._
import org.scalatestplus.play.PlaySpec
import org.joda.time.{DateTime, DateTimeZone, LocalTime, MonthDay}

class RecurrenceSpec extends PlaySpec {

  val fivePM = LocalTime.parse("17:00:00")

  val timeZone = DateTimeZone.forID("America/Toronto")

  val justMonday = Seq(DayOfWeek.MONDAY)
  val justWednesday = Seq(DayOfWeek.WEDNESDAY)
  val mwf = Seq(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)

  "Hourly" should {

    "recur every 2h on the 42nd minute" in  {
      val recurrence = Hourly(2, 42)
      recurrence.nextAfter(DateTime.parse("2010-06-07T09:42")) mustBe DateTime.parse("2010-06-07T11:42")
    }

    "recur later the same hour" in {
      val recurrence = Hourly(1, 42)
      recurrence.nextAfter(DateTime.parse("2010-06-07T09:40")) mustBe DateTime.parse("2010-06-07T09:42")
    }

    "recur the next hour if past minute of hour" in {
      val recurrence = Hourly(1, 42)
      recurrence.nextAfter(DateTime.parse("2010-06-07T09:43")) mustBe DateTime.parse("2010-06-07T10:42")
    }

    "have the right initial time when earlier in hour" in {
      val recurrence = Hourly(2, 42)
      recurrence.initialAfter(DateTime.parse("2010-06-07T09:41")) mustBe DateTime.parse("2010-06-07T09:42")
    }

    "have the right initial time when later in hour" in {
      val recurrence = Hourly(2, 42)
      recurrence.initialAfter(DateTime.parse("2010-06-07T09:43")) mustBe DateTime.parse("2010-06-07T10:42")
    }

    "have the right initial time when on same minute" in {
      val recurrence = Hourly(2, 42)
      recurrence.initialAfter(DateTime.parse("2010-06-07T09:42")) mustBe DateTime.parse("2010-06-07T09:42")
    }

    "be created with implied frequency of 1" in {
      Recurrence.maybeFromText("every hour", timeZone) mustBe Some(Hourly(1, DateTime.now.getMinuteOfHour))
    }

    "be created with frequency" in {
      Recurrence.maybeFromText("every 4 hours", timeZone) mustBe Some(Hourly(4, DateTime.now.getMinuteOfHour))
    }

    "be created with frequency and minutes" in {
      Recurrence.maybeFromText("every 4 hours at 15 minutes", timeZone) mustBe Some(Hourly(4, 15))
    }

  }

  "Daily" should {

    "recur every day at noon" in  {
      val recurrence = Daily(1, LocalTime.parse("12:00:00"), timeZone)
      recurrence.nextAfter(new DateTime(2010, 6, 7, 12, 0, timeZone)) mustBe new DateTime(2010, 6, 8, 12, 0, timeZone)
    }

    "recur later the same day" in  {
      val recurrence = Daily(1, LocalTime.parse("12:00:00"), timeZone)
      recurrence.nextAfter(new DateTime(2010, 6, 7, 11, 50, timeZone)) mustBe new DateTime(2010, 6, 7, 12, 0, timeZone)
    }

    "recur later the next day if already past the target time" in  {
      val recurrence = Daily(1, LocalTime.parse("12:00:00"), timeZone)
      recurrence.nextAfter(new DateTime(2010, 6, 7, 12, 50, timeZone)) mustBe new DateTime(2010, 6, 8, 12, 0, timeZone)
    }

    "have the right initial time when earlier in the day" in {
      val recurrence = Daily(2, LocalTime.parse("12:00:00"), timeZone)
      recurrence.initialAfter(new DateTime(2010, 6, 7, 11, 59, timeZone)) mustBe new DateTime(2010, 6, 7, 12, 0, timeZone)
    }

    "have the right initial time when later in the day" in {
      val recurrence = Daily(2, LocalTime.parse("12:00:00"), timeZone)
      recurrence.initialAfter(new DateTime(2010, 6, 7, 12, 1, timeZone)) mustBe new DateTime(2010, 6, 8, 12, 0, timeZone)
    }

    "have the right initial time when at the same point in the day" in {
      val recurrence = Daily(2, LocalTime.parse("12:00:00"), timeZone)
      recurrence.initialAfter(new DateTime(2010, 6, 7, 12, 0, timeZone)) mustBe new DateTime(2010, 6, 7, 12, 0, timeZone)
    }

    "be created with implied frequency of 1" in {
      Recurrence.maybeFromText("every day", timeZone) mustBe Some(Daily(1, Recurrence.currentAdjustedTime(timeZone), timeZone))
    }

    "be created with frequency" in {
      Recurrence.maybeFromText("every 4 days", timeZone) mustBe Some(Daily(4, Recurrence.currentAdjustedTime(timeZone), timeZone))
    }

    "be created with frequency and time" in {
      Recurrence.maybeFromText("every 4 days at 3pm", timeZone) mustBe Some(Daily(4, LocalTime.parse("15:00"), timeZone))
    }

    "use the timezone to determine the time, if specified, but save with default timezone" in {
      Recurrence.maybeFromText("every 4 days at 3pm pacific", timeZone) mustBe Some(Daily(4, LocalTime.parse("18:00"), timeZone))
    }

    "use the default timezone if not specified in recurrence" in {
      val laTz = DateTimeZone.forID("America/Los_Angeles")
      Recurrence.maybeFromText("every 4 days at 3pm", laTz) mustBe Some(Daily(4, LocalTime.parse("15:00"), laTz))
    }

  }

  "Weekly" should {

    "recur every second week at Monday, 2pm" in  {
      val recurrence = Weekly(2, justMonday, LocalTime.parse("14:00:00"), timeZone) // 1 is Monday
      recurrence.nextAfter(new DateTime(2010, 6, 7, 14, 0, timeZone)) mustBe new DateTime(2010, 6, 21, 14, 0, timeZone)
    }

    "recur later in the week" in  {
      val recurrence = Weekly(1, justWednesday, fivePM, timeZone)
      recurrence.nextAfter(new DateTime(2010, 6, 8, 12, 1, timeZone)) mustBe new DateTime(2010, 6, 9, 17, 0, timeZone)
    }

    "recur the following week if already past target day" in  {
      val recurrence = Weekly(1, justWednesday, fivePM, timeZone)
      recurrence.nextAfter(new DateTime(2010, 6, 10, 12, 1, timeZone)) mustBe new DateTime(2010, 6, 16, 17, 0, timeZone)
    }

    "recur the next of multiple days in the week, if there is one" in {
      val recurrence = Weekly(1, mwf, fivePM, timeZone)
      recurrence.nextAfter(new DateTime(2010, 6, 8, 12, 1, timeZone)) mustBe new DateTime(2010, 6, 9, 17, 0, timeZone)
    }

    "recur the following week if past all days in the week" in {
      val recurrence = Weekly(1, mwf, fivePM, timeZone)
      recurrence.nextAfter(new DateTime(2010, 6, 12, 12, 1, timeZone)) mustBe new DateTime(2010, 6, 14, 17, 0, timeZone)
    }

    "have the right initial time when earlier in the week" in {
      val recurrence = Weekly(2, justWednesday, fivePM, timeZone)
      recurrence.initialAfter(new DateTime(2010, 6, 9, 16, 59, timeZone)) mustBe new DateTime(2010, 6, 9, 17, 0, timeZone)
    }

    "have the right initial time when later in the week" in {
      val recurrence = Weekly(2, justWednesday, fivePM, timeZone)
      recurrence.initialAfter(new DateTime(2010, 6, 9, 17, 1, timeZone)) mustBe new DateTime(2010, 6, 16, 17, 0, timeZone)
    }

    "have the right initial time when at the same point in the week" in {
      val recurrence = Weekly(2, justWednesday, fivePM, timeZone)
      recurrence.initialAfter(new DateTime(2010, 6, 9, 17, 0, timeZone)) mustBe new DateTime(2010, 6, 9, 17, 0, timeZone)
    }

    "be created with implied frequency of 1" in {
      Recurrence.maybeFromText("every week", timeZone) mustBe Some(Weekly(1, Seq(DayOfWeek.of(DateTime.now.getDayOfWeek)), Recurrence.currentAdjustedTime(timeZone), timeZone))
    }

    "be created with frequency" in {
      Recurrence.maybeFromText("every 2 weeks", timeZone) mustBe Some(Weekly(2, Seq(DayOfWeek.of(DateTime.now.getDayOfWeek)), Recurrence.currentAdjustedTime(timeZone), timeZone))
    }

    "be created with frequency, day of week and time" in {
      Recurrence.maybeFromText("every 2 weeks on Monday at 3pm", timeZone) mustBe Some(Weekly(2, justMonday, LocalTime.parse("15:00"), timeZone))
    }

    "be created with frequency, multiple days of week and time" in {
      Recurrence.maybeFromText("every 2 weeks on Monday Wednesday Friday at 3pm", timeZone) mustBe
        Some(Weekly(2, mwf, LocalTime.parse("15:00"), timeZone))
    }

  }

  "MonthlyByDayOfMonth" should {

    "recur the first of every second month, at 5pm" in  {
      val recurrence = MonthlyByDayOfMonth(2, 1, fivePM, timeZone)
      recurrence.nextAfter(new DateTime(2010, 6, 7, 12, 1, timeZone)) mustBe new DateTime(2010, 8, 1, 17, 0, timeZone)
    }

    "recur later in the month if starting from earlier day in the month" in  {
      val recurrence = MonthlyByDayOfMonth(1, 6, fivePM, timeZone)
      recurrence.nextAfter(new DateTime(2010, 6, 5, 12, 1, timeZone)) mustBe new DateTime(2010, 6, 6, 17, 0, timeZone)
    }

    "recur later in the day if starting from earlier time in target day" in  {
      val recurrence = MonthlyByDayOfMonth(1, 6, fivePM, timeZone)
      recurrence.nextAfter(new DateTime(2010, 6, 6, 12, 1, timeZone)) mustBe new DateTime(2010, 6, 6, 17, 0, timeZone)
    }

    "recur next month if starting later in the month" in  {
      val recurrence = MonthlyByDayOfMonth(1, 6, fivePM, timeZone)
      recurrence.nextAfter(new DateTime(2010, 6, 6, 17, 1, timeZone)) mustBe new DateTime(2010, 7, 6, 17, 0, timeZone)
    }

    "have the right initial time when earlier in the month" in {
      val recurrence = MonthlyByDayOfMonth(1, 6, fivePM, timeZone)
      recurrence.initialAfter(new DateTime(2010, 6, 6, 16, 59, timeZone)) mustBe new DateTime(2010, 6, 6, 17, 0, timeZone)
    }

    "have the right initial time when later in the month" in {
      val recurrence = MonthlyByDayOfMonth(1, 6, fivePM, timeZone)
      recurrence.initialAfter(new DateTime(2010, 6, 6, 17, 1, timeZone)) mustBe new DateTime(2010, 7, 6, 17, 0, timeZone)
    }

    "have the right initial time when at the same point in the month" in {
      val recurrence = MonthlyByDayOfMonth(1, 6, fivePM, timeZone)
      recurrence.initialAfter(new DateTime(2010, 6, 6, 17, 0, timeZone)) mustBe new DateTime(2010, 6, 6, 17, 0, timeZone)
    }

    "be created with first day of every month" in {
      Recurrence.maybeFromText("the first day of every month at 9am", timeZone) mustBe Some(MonthlyByDayOfMonth(1, 1, LocalTime.parse("09:00"), timeZone))
    }

    "be created with 15th day of every 3rd month" in {
      Recurrence.maybeFromText("the 15th of every 3rd month at 5pm", timeZone) mustBe Some(MonthlyByDayOfMonth(3, 15, fivePM, timeZone))
    }

  }

  "MonthlyByNthDayOfWeek" should {

    "recur the second Tuesday of every month, at 5pm" in  {
      val recurrence = MonthlyByNthDayOfWeek(1, DayOfWeek.TUESDAY, 2, fivePM, timeZone)
      recurrence.nextAfter(new DateTime(2010, 6, 7, 12, 1, timeZone)) mustBe new DateTime(2010, 6, 8, 17, 0, timeZone)
    }

    "recur correctly when month starts with the target day of week" in  {
      val recurrence = MonthlyByNthDayOfWeek(1, DayOfWeek.THURSDAY, 1, fivePM, timeZone)
      recurrence.nextAfter(new DateTime(2010, 6, 7, 12, 1, timeZone)) mustBe new DateTime(2010, 7, 1, 17, 0, timeZone)
    }

    "recur correctly when month starts with a day before the target day of week" in  {
      val recurrence = MonthlyByNthDayOfWeek(1, DayOfWeek.FRIDAY, 1, fivePM, timeZone)
      recurrence.nextAfter(new DateTime(2010, 6, 7, 12, 1, timeZone)) mustBe new DateTime(2010, 7, 2, 17, 0, timeZone)
    }

    "recur correctly when month starts with a day after the target day of week" in  {
      val recurrence = MonthlyByNthDayOfWeek(1, DayOfWeek.WEDNESDAY, 1, fivePM, timeZone)
      recurrence.nextAfter(new DateTime(2010, 6, 7, 12, 1, timeZone)) mustBe new DateTime(2010, 7, 7, 17, 0, timeZone)
    }

    "recur later the same month if starting from earlier in the month" in  {
      val recurrence = MonthlyByNthDayOfWeek(1, DayOfWeek.MONDAY, 1, fivePM, timeZone)
      recurrence.nextAfter(new DateTime(2010, 6, 6, 12, 1, timeZone)) mustBe new DateTime(2010, 6, 7, 17, 0, timeZone)
    }

    "recur later the same month if starting from earlier in the target day" in  {
      val recurrence = MonthlyByNthDayOfWeek(1, DayOfWeek.MONDAY, 1, fivePM, timeZone)
      recurrence.nextAfter(new DateTime(2010, 6, 7, 12, 1, timeZone)) mustBe new DateTime(2010, 6, 7, 17, 0, timeZone)
    }

    "have the right initial time when earlier in the month" in {
      val recurrence = MonthlyByNthDayOfWeek(1, DayOfWeek.MONDAY, 1, fivePM, timeZone)
      recurrence.initialAfter(new DateTime(2010, 6, 7, 16, 59, timeZone)) mustBe new DateTime(2010, 6, 7, 17, 0, timeZone)
    }

    "have the right initial time when later in the month" in {
      val recurrence = MonthlyByNthDayOfWeek(1, DayOfWeek.MONDAY, 1, fivePM, timeZone)
      recurrence.initialAfter(new DateTime(2010, 6, 7, 17, 1, timeZone)) mustBe new DateTime(2010, 7, 5, 17, 0, timeZone)
    }

    "have the right initial time when at the same point in the month" in {
      val recurrence = MonthlyByNthDayOfWeek(1, DayOfWeek.MONDAY, 1, fivePM, timeZone)
      recurrence.initialAfter(new DateTime(2010, 6, 7, 17, 0, timeZone)) mustBe new DateTime(2010, 6, 7, 17, 0, timeZone)
    }

    "be created with first monday of every month" in {
      Recurrence.maybeFromText("the first monday of every month at 5pm", timeZone) mustBe
        Some(MonthlyByNthDayOfWeek(1, DayOfWeek.MONDAY, 1, fivePM, timeZone))
    }

    "be created with 2nd wednesday of every 3rd month" in {
      Recurrence.maybeFromText("the 2nd wednesday of every 3rd month at 5pm", timeZone) mustBe
        Some(MonthlyByNthDayOfWeek(3, DayOfWeek.WEDNESDAY, 2, fivePM, timeZone))
    }

  }

  "Yearly" should {

    "recur Jan 14 every year, at 5pm" in  {
      val recurrence = Yearly(1, new MonthDay(1, 14), fivePM, timeZone)
      recurrence.nextAfter(new DateTime(2010, 6, 7, 12, 1, timeZone)) mustBe new DateTime(2011, 1, 14, 17, 0, timeZone)
    }

    "recur later the same year" in  {
      val recurrence = Yearly(1, new MonthDay(1, 14), fivePM, timeZone)
      recurrence.nextAfter(new DateTime(2010, 1, 13, 12, 1, timeZone)) mustBe new DateTime(2010, 1, 14, 17, 0, timeZone)
    }

    "recur later the same day" in  {
      val recurrence = Yearly(1, new MonthDay(1, 14), fivePM, timeZone)
      recurrence.nextAfter(new DateTime(2010, 1, 14, 12, 1, timeZone)) mustBe new DateTime(2010, 1, 14, 17, 0, timeZone)
    }

    "recur the next year" in  {
      val recurrence = Yearly(1, new MonthDay(1, 14), fivePM, timeZone)
      recurrence.nextAfter(new DateTime(2010, 1, 14, 17, 1, timeZone)) mustBe new DateTime(2011, 1, 14, 17, 0, timeZone)
    }

    "have the right initial time when earlier in the year" in {
      val recurrence = Yearly(1, new MonthDay(1, 14), fivePM, timeZone)
      recurrence.initialAfter(new DateTime(2010, 1, 14, 16, 59, timeZone)) mustBe new DateTime(2010, 1, 14, 17, 0, timeZone)
    }

    "have the right initial time when later in the year" in {
      val recurrence = Yearly(1, new MonthDay(1, 14), fivePM, timeZone)
      recurrence.initialAfter(new DateTime(2010, 1, 14, 17, 1, timeZone)) mustBe new DateTime(2011, 1, 14, 17, 0, timeZone)
    }

    "have the right initial time when at the same point in the year" in {
      val recurrence = Yearly(1, new MonthDay(1, 14), fivePM, timeZone)
      recurrence.initialAfter(new DateTime(2010, 1, 14, 17, 0, timeZone)) mustBe new DateTime(2010, 1, 14, 17, 0, timeZone)
    }

    "be created for the 14th of January every year" in {
      Recurrence.maybeFromText("every year on January 14 at 5pm", timeZone) mustBe Some(Yearly(1, new MonthDay(1, 14), fivePM, timeZone))
    }

    "be created for every second January 14th with no time specified" in {
      val time = Recurrence.currentAdjustedTime(timeZone)
      Recurrence.maybeFromText("every 2nd year on January 14", timeZone) mustBe Some(Yearly(2, new MonthDay(1, 14), time, timeZone))
    }

  }

  "Recurrence.daysOfWeekFrom" should {

    val monday = DayOfWeek.MONDAY.getValue
    val wednesday = DayOfWeek.WEDNESDAY.getValue
    val friday = DayOfWeek.FRIDAY.getValue

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
  }



}
