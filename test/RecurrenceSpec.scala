import models.bots.triggers._
import org.scalatestplus.play.PlaySpec
import org.joda.time._

class RecurrenceSpec extends PlaySpec {

  val fivePM = LocalTime.parse("17:00:00")

  "Hourly" should {

    "recur every 2h on the 42nd minute" in  {
      val recurrence = Hourly(2, 42)
      recurrence.nextAfter(DateTime.parse("2010-06-07T09:43")) mustBe DateTime.parse("2010-06-07T11:42")
    }

    "recur later the same hour" in {
      val recurrence = Hourly(1, 42)
      recurrence.nextAfter(DateTime.parse("2010-06-07T09:40")) mustBe DateTime.parse("2010-06-07T09:42")
    }

    "recur the next hour if past minute of hour" in {
      val recurrence = Hourly(1, 42)
      recurrence.nextAfter(DateTime.parse("2010-06-07T09:43")) mustBe DateTime.parse("2010-06-07T10:42")
    }

  }

  "Daily" should {

    "recur every day at noon" in  {
      val recurrence = Daily(1, LocalTime.parse("12:00:00"))
      recurrence.nextAfter(DateTime.parse("2010-06-07T12:01")) mustBe DateTime.parse("2010-06-08T12:00")
    }

    "recur later the same day" in  {
      val recurrence = Daily(1, LocalTime.parse("12:00:00"))
      recurrence.nextAfter(DateTime.parse("2010-06-07T11:50")) mustBe DateTime.parse("2010-06-07T12:00")
    }

    "recur later the next day if already past the target time" in  {
      val recurrence = Daily(1, LocalTime.parse("12:00:00"))
      recurrence.nextAfter(DateTime.parse("2010-06-07T12:50")) mustBe DateTime.parse("2010-06-08T12:00")
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

  }



}
