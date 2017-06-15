package json

import java.time.{DayOfWeek, LocalTime, MonthDay, ZoneId}
import java.time.format.TextStyle
import java.util.Locale

import models.IDs
import models.behaviors.scheduling.recurrence._
import services.DataService
import utils.TimeZoneParser

case class ScheduledActionRecurrenceTimeData(hour: Int, minute: Int)

case class ScheduledActionRecurrenceData(
                                          displayString: String,
                                          frequency: Int,
                                          typeName: String,
                                          timeOfDay: Option[ScheduledActionRecurrenceTimeData],
                                          timeZone: Option[String],
                                          timeZoneName: Option[String],
                                          minuteOfHour: Option[Int],
                                          dayOfWeek: Option[Int],
                                          dayOfMonth: Option[Int],
                                          nthDayOfWeek: Option[Int],
                                          month: Option[Int],
                                          daysOfWeek: Seq[Int]
                                        ) {

  private def maybeValidMinuteOfHour: Option[Int] = {
    minuteOfHour.filter(ea => ea >= 0 && ea <= 59)
  }

  private def maybeValidDayOfWeekList: Option[Seq[DayOfWeek]] = {
    try {
      Some(daysOfWeek.map(DayOfWeek.of)).filter(_.nonEmpty)
    } catch {
      case _: Exception => None
    }
  }

  private def maybeValidDayOfWeek: Option[DayOfWeek] = {
    try {
      dayOfWeek.map(DayOfWeek.of)
    } catch {
      case _: Exception => None
    }
  }

  private def maybeValidDayOfMonth: Option[Int] = {
    dayOfMonth.filter(ea => ea >= 1 && ea <= 31)
  }

  private def maybeValidNthDayOfWeek: Option[Int] = {
    nthDayOfWeek.filter(ea => ea >= 1 && ea <= 5)
  }

  private def maybeValidMonthDay: Option[MonthDay] = {
    try {
      for {
        month <- month
        dayOfMonth <- dayOfMonth
      } yield {
        MonthDay.of(month, dayOfMonth)
      }
    } catch {
      case _: Exception => None
    }
  }

  private def maybeLocalTime: Option[LocalTime] = {
    try {
      timeOfDay.map(ea => LocalTime.of(ea.hour, ea.minute))
    } catch {
      case _: Exception => None
    }
  }

  private def maybeZoneId: Option[ZoneId] = {
    timeZone.flatMap(TimeZoneParser.maybeZoneForId)
  }

  def maybeToMinutely: Option[Minutely] = {
    for {
      _ <- Option(typeName).filter(_ == Minutely.recurrenceType)
    } yield {
      Minutely(IDs.next, frequency)
    }
  }

  def maybeToHourly: Option[Hourly] = {
    for {
      _ <- Option(typeName).filter(_ == Hourly.recurrenceType)
      validMinuteOfHour <- maybeValidMinuteOfHour
    } yield {
      Hourly(IDs.next, frequency, validMinuteOfHour)
    }
  }

  def maybeToDaily: Option[Daily] = {
    for {
      _ <- Option(typeName).filter(_ == Daily.recurrenceType)
      localTime <- maybeLocalTime
      timeZone <- maybeZoneId
    } yield {
      Daily(IDs.next, frequency, localTime, timeZone)
    }
  }

  def maybeToWeekly: Option[Weekly] = {
    for {
      _ <- Option(typeName).filter(_ == Weekly.recurrenceType)
      daysOfWeek <- maybeValidDayOfWeekList
      localTime <- maybeLocalTime
      zoneId <- maybeZoneId
    } yield {
      Weekly(IDs.next, frequency, daysOfWeek, localTime, zoneId)
    }
  }

  def maybeToMonthlyByDayOfMonth: Option[MonthlyByDayOfMonth] = {
    for {
      _ <- Option(typeName).filter(_ == MonthlyByDayOfMonth.recurrenceType)
      dayOfMonth <- maybeValidDayOfMonth
      localTime <- maybeLocalTime
      zoneId <- maybeZoneId
    } yield {
      MonthlyByDayOfMonth(IDs.next, frequency, dayOfMonth, localTime, zoneId)
    }
  }

  def maybeToMonthlyByNthDayOfWeek: Option[MonthlyByNthDayOfWeek] = {
    for {
      _ <- Option(typeName).filter(_ == MonthlyByNthDayOfWeek.recurrenceType)
      dayOfWeek <- maybeValidDayOfWeek
      nthDayOfWeek <- maybeValidNthDayOfWeek
      localTime <- maybeLocalTime
      zoneId <- maybeZoneId
    } yield {
      MonthlyByNthDayOfWeek(IDs.next, frequency, dayOfWeek, nthDayOfWeek, localTime, zoneId)
    }
  }

  def maybeToYearly: Option[Yearly] = {
    for {
      _ <- Option(typeName).filter(_ == Yearly.recurrenceType)
      monthDay <- maybeValidMonthDay
      localTime <- maybeLocalTime
      zoneId <- maybeZoneId
    } yield {
      Yearly(IDs.next, frequency, monthDay, localTime, zoneId)
    }
  }

  def maybeNewRecurrence: Option[Recurrence] = {
    this.maybeToMinutely.orElse {
      this.maybeToHourly.orElse {
        this.maybeToDaily.orElse {
          this.maybeToWeekly.orElse {
            this.maybeToMonthlyByDayOfMonth.orElse {
              this.maybeToMonthlyByNthDayOfWeek.orElse {
                this.maybeToYearly.orElse {
                  None
                }
              }
            }
          }
        }
      }
    }
  }
}

object ScheduledActionRecurrenceData {
  def fromRecurrence(recurrence: Recurrence): ScheduledActionRecurrenceData = {
    ScheduledActionRecurrenceData(
      recurrence.displayString,
      recurrence.frequency,
      recurrence.typeName,
      recurrence.maybeTimeOfDay.map(time => ScheduledActionRecurrenceTimeData(time.getHour, time.getMinute)),
      recurrence.maybeTimeZone.map(_.toString),
      recurrence.maybeTimeZone.map(_.getDisplayName(TextStyle.FULL, Locale.ENGLISH)),
      recurrence.maybeMinuteOfHour,
      recurrence.maybeDayOfWeek.map(_.getValue),
      recurrence.maybeDayOfMonth,
      recurrence.maybeNthDayOfWeek,
      recurrence.maybeMonth,
      recurrence.daysOfWeek.map(_.getValue)
    )
  }
}
