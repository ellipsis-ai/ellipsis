import models.behaviors.scheduledmessage._
import org.scalatestplus.play.PlaySpec
import org.joda.time.{DateTime, DateTimeZone, LocalTime, MonthDay}

class RecurrenceSpec extends PlaySpec {

  val fivePM = LocalTime.parse("17:00:00")

  val timeZone = DateTimeZone.forID("America/Toronto")

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
      val recurrence = Daily(1, LocalTime.parse("12:00:00"))
      recurrence.nextAfter(DateTime.parse("2010-06-07T12:00")) mustBe DateTime.parse("2010-06-08T12:00")
    }

    "recur later the same day" in  {
      val recurrence = Daily(1, LocalTime.parse("12:00:00"))
      recurrence.nextAfter(DateTime.parse("2010-06-07T11:50")) mustBe DateTime.parse("2010-06-07T12:00")
    }

    "recur later the next day if already past the target time" in  {
      val recurrence = Daily(1, LocalTime.parse("12:00:00"))
      recurrence.nextAfter(DateTime.parse("2010-06-07T12:50")) mustBe DateTime.parse("2010-06-08T12:00")
    }

    "have the right initial time when earlier in the day" in {
      val recurrence = Daily(2, LocalTime.parse("12:00:00"))
      recurrence.initialAfter(DateTime.parse("2010-06-07T11:59")) mustBe DateTime.parse("2010-06-07T12:00")
    }

    "have the right initial time when later in the day" in {
      val recurrence = Daily(2, LocalTime.parse("12:00:00"))
      recurrence.initialAfter(DateTime.parse("2010-06-07T12:01")) mustBe DateTime.parse("2010-06-08T12:00")
    }

    "have the right initial time when at the same point in the day" in {
      val recurrence = Daily(2, LocalTime.parse("12:00:00"))
      recurrence.initialAfter(DateTime.parse("2010-06-07T12:00")) mustBe DateTime.parse("2010-06-07T12:00")
    }

    "be created with implied frequency of 1" in {
      Recurrence.maybeFromText("every day", timeZone) mustBe Some(Daily(1, Recurrence.currentAdjustedTime))
    }

    "be created with frequency" in {
      Recurrence.maybeFromText("every 4 days", timeZone) mustBe Some(Daily(4, Recurrence.currentAdjustedTime))
    }

    "be created with frequency and time" in {
      Recurrence.maybeFromText("every 4 days at 3pm", timeZone) mustBe Some(Daily(4, LocalTime.parse("15:00")))
    }

    "use the timezone if specified" in {
      Recurrence.maybeFromText("every 4 days at 3pm pacific", timeZone) mustBe Some(Daily(4, LocalTime.parse("18:00")))
    }

    "use the default timezone if not specified" in {
      val laTz = DateTimeZone.forID("America/Los_Angeles")
      Recurrence.maybeFromText("every 4 days at 3pm", laTz) mustBe Some(Daily(4, LocalTime.parse("18:00")))
    }

  }

  "Weekly" should {

    "recur every second week at Monday, 2pm" in  {
      val recurrence = Weekly(2, 1, LocalTime.parse("14:00:00")) // 1 is Monday
      recurrence.nextAfter(DateTime.parse("2010-06-07T14:00")) mustBe DateTime.parse("2010-06-21T14:00")
    }

    "recur later in the week" in  {
      val recurrence = Weekly(1, 3, fivePM)
      recurrence.nextAfter(DateTime.parse("2010-06-08T12:01")) mustBe DateTime.parse("2010-06-09T17:00")
    }

    "recur the following week if already past target day" in  {
      val recurrence = Weekly(1, 3, fivePM)
      recurrence.nextAfter(DateTime.parse("2010-06-10T12:01")) mustBe DateTime.parse("2010-06-16T17:00")
    }

    "have the right initial time when earlier in the week" in {
      val recurrence = Weekly(2, 3, fivePM)
      recurrence.initialAfter(DateTime.parse("2010-06-09T16:59")) mustBe DateTime.parse("2010-06-09T17:00")
    }

    "have the right initial time when later in the week" in {
      val recurrence = Weekly(2, 3, fivePM)
      recurrence.initialAfter(DateTime.parse("2010-06-09T17:01")) mustBe DateTime.parse("2010-06-16T17:00")
    }

    "have the right initial time when at the same point in the week" in {
      val recurrence = Weekly(2, 3, fivePM)
      recurrence.initialAfter(DateTime.parse("2010-06-09T17:00")) mustBe DateTime.parse("2010-06-09T17:00")
    }

    "be created with implied frequency of 1" in {
      Recurrence.maybeFromText("every week", timeZone) mustBe Some(Weekly(1, DateTime.now.getDayOfWeek, Recurrence.currentAdjustedTime))
    }

    "be created with frequency" in {
      Recurrence.maybeFromText("every 2 weeks", timeZone) mustBe Some(Weekly(2, DateTime.now.getDayOfWeek, Recurrence.currentAdjustedTime))
    }

    "be created with frequency, day of week and time" in {
      Recurrence.maybeFromText("every 2 weeks on Monday at 3pm", timeZone) mustBe Some(Weekly(2, 1, LocalTime.parse("15:00")))
    }

  }

  "MonthlyByDayOfMonth" should {

    "recur the first of every second month, at 5pm" in  {
      val recurrence = MonthlyByDayOfMonth(2, 1, fivePM)
      recurrence.nextAfter(DateTime.parse("2010-06-07T12:01")) mustBe DateTime.parse("2010-08-01T17:00")
    }

    "recur later in the month if starting from earlier day in the month" in  {
      val recurrence = MonthlyByDayOfMonth(1, 6, fivePM)
      recurrence.nextAfter(DateTime.parse("2010-06-05T12:01")) mustBe DateTime.parse("2010-06-06T17:00")
    }

    "recur later in the day if starting from earlier time in target day" in  {
      val recurrence = MonthlyByDayOfMonth(1, 6, fivePM)
      recurrence.nextAfter(DateTime.parse("2010-06-06T12:01")) mustBe DateTime.parse("2010-06-06T17:00")
    }

    "recur next month if starting later in the month" in  {
      val recurrence = MonthlyByDayOfMonth(1, 6, fivePM)
      recurrence.nextAfter(DateTime.parse("2010-06-06T17:01")) mustBe DateTime.parse("2010-07-06T17:00")
    }

    "have the right initial time when earlier in the month" in {
      val recurrence = MonthlyByDayOfMonth(1, 6, fivePM)
      recurrence.initialAfter(DateTime.parse("2010-06-06T16:59")) mustBe DateTime.parse("2010-06-06T17:00")
    }

    "have the right initial time when later in the month" in {
      val recurrence = MonthlyByDayOfMonth(1, 6, fivePM)
      recurrence.initialAfter(DateTime.parse("2010-06-06T17:01")) mustBe DateTime.parse("2010-07-06T17:00")
    }

    "have the right initial time when at the same point in the month" in {
      val recurrence = MonthlyByDayOfMonth(1, 6, fivePM)
      recurrence.initialAfter(DateTime.parse("2010-06-06T17:00")) mustBe DateTime.parse("2010-06-06T17:00")
    }

    "be created with first day of every month" in {
      Recurrence.maybeFromText("the first day of every month at 9am", timeZone) mustBe Some(MonthlyByDayOfMonth(1, 1, LocalTime.parse("09:00")))
    }

    "be created with 15th day of every 3rd month" in {
      Recurrence.maybeFromText("the 15th of every 3rd month at 5pm", timeZone) mustBe Some(MonthlyByDayOfMonth(3, 15, fivePM))
    }

  }

  "MonthlyByNthDayOfWeek" should {

    "recur the second Tuesday of every month, at 5pm" in  {
      val recurrence = MonthlyByNthDayOfWeek(1, 2, 2, fivePM)
      recurrence.nextAfter(DateTime.parse("2010-06-07T12:01")) mustBe DateTime.parse("2010-06-08T17:00")
    }

    "recur correctly when month starts with the target day of week" in  {
      val recurrence = MonthlyByNthDayOfWeek(1, 4, 1, fivePM)
      recurrence.nextAfter(DateTime.parse("2010-06-07T12:01")) mustBe DateTime.parse("2010-07-01T17:00")
    }

    "recur correctly when month starts with a day before the target day of week" in  {
      val recurrence = MonthlyByNthDayOfWeek(1, 5, 1, fivePM)
      recurrence.nextAfter(DateTime.parse("2010-06-07T12:01")) mustBe DateTime.parse("2010-07-02T17:00")
    }

    "recur correctly when month starts with a day after the target day of week" in  {
      val recurrence = MonthlyByNthDayOfWeek(1, 3, 1, fivePM)
      recurrence.nextAfter(DateTime.parse("2010-06-07T12:01")) mustBe DateTime.parse("2010-07-07T17:00")
    }

    "recur later the same month if starting from earlier in the month" in  {
      val recurrence = MonthlyByNthDayOfWeek(1, 1, 1, fivePM)
      recurrence.nextAfter(DateTime.parse("2010-06-06T12:01")) mustBe DateTime.parse("2010-06-07T17:00")
    }

    "recur later the same month if starting from earlier in the target day" in  {
      val recurrence = MonthlyByNthDayOfWeek(1, 1, 1, fivePM)
      recurrence.nextAfter(DateTime.parse("2010-06-07T12:01")) mustBe DateTime.parse("2010-06-07T17:00")
    }

    "have the right initial time when earlier in the month" in {
      val recurrence = MonthlyByNthDayOfWeek(1, 1, 1, fivePM)
      recurrence.initialAfter(DateTime.parse("2010-06-07T16:59")) mustBe DateTime.parse("2010-06-07T17:00")
    }

    "have the right initial time when later in the month" in {
      val recurrence = MonthlyByNthDayOfWeek(1, 1, 1, fivePM)
      recurrence.initialAfter(DateTime.parse("2010-06-07T17:01")) mustBe DateTime.parse("2010-07-05T17:00")
    }

    "have the right initial time when at the same point in the month" in {
      val recurrence = MonthlyByNthDayOfWeek(1, 1, 1, fivePM)
      recurrence.initialAfter(DateTime.parse("2010-06-07T17:00")) mustBe DateTime.parse("2010-06-07T17:00")
    }

    "be created with first monday of every month" in {
      Recurrence.maybeFromText("the first monday of every month at 5pm", timeZone) mustBe Some(MonthlyByNthDayOfWeek(1, 1, 1, fivePM))
    }

    "be created with 2nd wednesday of every 3rd month" in {
      Recurrence.maybeFromText("the 2nd wednesday of every 3rd month at 5pm", timeZone) mustBe Some(MonthlyByNthDayOfWeek(3, 3, 2, fivePM))
    }

  }

  "Yearly" should {

    "recur Jan 14 every year, at 5pm" in  {
      val recurrence = Yearly(1, new MonthDay(1, 14), fivePM)
      recurrence.nextAfter(DateTime.parse("2010-06-07T12:01")) mustBe DateTime.parse("2011-01-14T17:00")
    }

    "recur later the same year" in  {
      val recurrence = Yearly(1, new MonthDay(1, 14), fivePM)
      recurrence.nextAfter(DateTime.parse("2010-01-13T12:01")) mustBe DateTime.parse("2010-01-14T17:00")
    }

    "recur later the same day" in  {
      val recurrence = Yearly(1, new MonthDay(1, 14), fivePM)
      recurrence.nextAfter(DateTime.parse("2010-01-14T12:01")) mustBe DateTime.parse("2010-01-14T17:00")
    }

    "recur the next year" in  {
      val recurrence = Yearly(1, new MonthDay(1, 14), fivePM)
      recurrence.nextAfter(DateTime.parse("2010-01-14T17:01")) mustBe DateTime.parse("2011-01-14T17:00")
    }

    "have the right initial time when earlier in the year" in {
      val recurrence = Yearly(1, new MonthDay(1, 14), fivePM)
      recurrence.initialAfter(DateTime.parse("2010-01-14T16:59")) mustBe DateTime.parse("2010-01-14T17:00")
    }

    "have the right initial time when later in the year" in {
      val recurrence = Yearly(1, new MonthDay(1, 14), fivePM)
      recurrence.initialAfter(DateTime.parse("2010-01-14T17:01")) mustBe DateTime.parse("2011-01-14T17:00")
    }

    "have the right initial time when at the same point in the year" in {
      val recurrence = Yearly(1, new MonthDay(1, 14), fivePM)
      recurrence.initialAfter(DateTime.parse("2010-01-14T17:00")) mustBe DateTime.parse("2010-01-14T17:00")
    }

    "be created for the 14th of January every year" in {
      Recurrence.maybeFromText("every year on January 14 at 5pm", timeZone) mustBe Some(Yearly(1, new MonthDay(1, 14), fivePM))
    }

    "be created for every second January 14th with no time specified" in {
      val time = Recurrence.currentAdjustedTime
      Recurrence.maybeFromText("every 2nd year on January 14", timeZone) mustBe Some(Yearly(2, new MonthDay(1, 14), time))
    }

  }



}
