import models.bots.triggers._
import org.scalatestplus.play.PlaySpec
import org.joda.time._

class RecurrenceSpec extends PlaySpec {

  "Hourly" should {

    "recur every 2h on the 42nd minute" in  {
      val recurrence = Hourly(2, 42)
      recurrence.nextAfter(DateTime.parse("2010-06-07T09:43")) mustBe DateTime.parse("2010-06-07T11:42")
    }

  }

  "Daily" should {

    "recur every day at noon" in  {
      val recurrence = Daily(1, LocalTime.parse("12:00:00"))
      recurrence.nextAfter(DateTime.parse("2010-06-07T12:01")) mustBe DateTime.parse("2010-06-08T12:00")
    }

  }

  "Weekly" should {

    "recur every second week at Monday, 2pm" in  {
      val recurrence = Weekly(2, 1, LocalTime.parse("14:00:00")) // 1 is Monday
      recurrence.nextAfter(DateTime.parse("2010-06-07T12:01")) mustBe DateTime.parse("2010-06-21T14:00")
    }

  }

  "MonthlyByDayOfMonth" should {

    "recur the first of every second month, at 5pm" in  {
      val recurrence = MonthlyByDayOfMonth(2, 1, LocalTime.parse("17:00:00"))
      recurrence.nextAfter(DateTime.parse("2010-06-07T12:01")) mustBe DateTime.parse("2010-08-01T17:00")
    }

  }

  "MonthlyByNthDayOfWeek" should {

    "recur the second Tuesday of every month, at 5pm" in  {
      val recurrence = MonthlyByNthDayOfWeek(1, 2, 2, LocalTime.parse("17:00:00"))
      recurrence.nextAfter(DateTime.parse("2010-06-07T12:01")) mustBe DateTime.parse("2010-07-13T17:00")
    }

    "recur correctly when month starts with the target day of week" in  {
      val recurrence = MonthlyByNthDayOfWeek(1, 4, 1, LocalTime.parse("17:00:00"))
      recurrence.nextAfter(DateTime.parse("2010-06-07T12:01")) mustBe DateTime.parse("2010-07-01T17:00")
    }

    "recur correctly when month starts with a day before the target day of week" in  {
      val recurrence = MonthlyByNthDayOfWeek(1, 5, 1, LocalTime.parse("17:00:00"))
      recurrence.nextAfter(DateTime.parse("2010-06-07T12:01")) mustBe DateTime.parse("2010-07-02T17:00")
    }

    "recur correctly when month starts with a day after the target day of week" in  {
      val recurrence = MonthlyByNthDayOfWeek(1, 3, 1, LocalTime.parse("17:00:00"))
      recurrence.nextAfter(DateTime.parse("2010-06-07T12:01")) mustBe DateTime.parse("2010-07-07T17:00")
    }

  }

  "Yearly" should {

    "recur Jan 14 every year, at 5pm" in  {
      val recurrence = Yearly(1, new MonthDay(1, 14), LocalTime.parse("17:00:00"))
      recurrence.nextAfter(DateTime.parse("2010-06-07T12:01")) mustBe DateTime.parse("2011-01-14T17:00")
    }

  }



}
