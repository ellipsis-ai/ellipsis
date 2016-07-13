import models.bots._
import org.scalatestplus.play.PlaySpec
import org.joda.time._

class RecurrenceSpec extends PlaySpec {

  val fivePM = LocalTime.parse("17:00:00")

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
      Recurrence.maybeFromText("every hour") mustBe Some(Hourly(1, DateTime.now.getMinuteOfHour))
    }

    "be created with frequency" in {
      Recurrence.maybeFromText("every 4 hours") mustBe Some(Hourly(4, DateTime.now.getMinuteOfHour))
    }

    "be created with frequency and minutes" in {
      Recurrence.maybeFromText("every 4 hours at 15 minutes") mustBe Some(Hourly(4, 15))
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
      Recurrence.maybeFromText("every day") mustBe Some(Daily(1, Recurrence.currentAdjustedTime))
    }

    "be created with frequency" in {
      Recurrence.maybeFromText("every 4 days") mustBe Some(Daily(4, Recurrence.currentAdjustedTime))
    }

    "be created with frequency and time" in {
      Recurrence.maybeFromText("every 4 days at 3pm") mustBe Some(Daily(4, LocalTime.parse("15:00")))
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
      Recurrence.maybeFromText("every week") mustBe Some(Weekly(1, DateTime.now.getDayOfWeek, Recurrence.currentAdjustedTime))
    }

    "be created with frequency" in {
      Recurrence.maybeFromText("every 2 weeks") mustBe Some(Weekly(2, DateTime.now.getDayOfWeek, Recurrence.currentAdjustedTime))
    }

    "be created with frequency, day of week and time" in {
      Recurrence.maybeFromText("every 2 weeks on Monday at 3pm") mustBe Some(Weekly(2, 1, LocalTime.parse("15:00")))
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

  }



}
