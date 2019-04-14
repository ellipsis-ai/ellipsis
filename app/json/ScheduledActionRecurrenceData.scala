package json

import java.time.format.TextStyle
import java.time._
import java.util.Locale

import models.IDs
import models.behaviors.scheduling.recurrence._
import utils.TimeZoneParser

case class ScheduledActionRecurrenceTimeData(hour: Int, minute: Int)

case class ScheduledActionRecurrenceData(
                                          id: Option[String],
                                          displayString: String,
                                          frequency: Int,
                                          timesHasRun: Int,
                                          totalTimesToRun: Option[Int],
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

  private def getId: String = {
    id.getOrElse(IDs.next)
  }

  private def maybeValidMinuteOfHour: Option[Int] = {
    minuteOfHour.filter(ea => ea >= 0 && ea <= 59)
  }

  private def maybeValidDayOfWeekList: Option[Seq[DayOfWeek]] = {
    try {
      Some(daysOfWeek.map(DayOfWeek.of)).filter(_.nonEmpty)
    } catch {
      case _: DateTimeException => None
    }
  }

  private def maybeValidDayOfWeek: Option[DayOfWeek] = {
    try {
      dayOfWeek.map(DayOfWeek.of)
    } catch {
      case _: DateTimeException => None
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
      case _: DateTimeException => None
    }
  }

  private def maybeLocalTime: Option[LocalTime] = {
    try {
      timeOfDay.map(ea => LocalTime.of(ea.hour, ea.minute))
    } catch {
      case _: DateTimeException => None
    }
  }

  private def maybeZoneId: Option[ZoneId] = {
    timeZone.flatMap(TimeZoneParser.maybeZoneForId)
  }

  def maybeToMinutely: Option[Minutely] = {
    for {
      _ <- Option(typeName).filter(_ == Minutely.recurrenceType)
    } yield {
      Minutely(getId, frequency, timesHasRun, totalTimesToRun)
    }
  }

  def maybeToHourly: Option[Hourly] = {
    for {
      _ <- Option(typeName).filter(_ == Hourly.recurrenceType)
      validMinuteOfHour <- maybeValidMinuteOfHour
      zoneId <- maybeZoneId
    } yield {
      Hourly(getId, frequency, timesHasRun, totalTimesToRun, validMinuteOfHour, zoneId)
    }
  }

  def maybeToDaily: Option[Daily] = {
    for {
      _ <- Option(typeName).filter(_ == Daily.recurrenceType)
      localTime <- maybeLocalTime
      zoneId <- maybeZoneId
    } yield {
      Daily(getId, frequency, timesHasRun, totalTimesToRun, localTime, zoneId)
    }
  }

  def maybeToWeekly: Option[Weekly] = {
    for {
      _ <- Option(typeName).filter(_ == Weekly.recurrenceType)
      daysOfWeek <- maybeValidDayOfWeekList
      localTime <- maybeLocalTime
      zoneId <- maybeZoneId
    } yield {
      Weekly(getId, frequency, timesHasRun, totalTimesToRun, daysOfWeek, localTime, zoneId)
    }
  }

  def maybeToMonthlyByDayOfMonth: Option[MonthlyByDayOfMonth] = {
    for {
      _ <- Option(typeName).filter(_ == MonthlyByDayOfMonth.recurrenceType)
      dayOfMonth <- maybeValidDayOfMonth
      localTime <- maybeLocalTime
      zoneId <- maybeZoneId
    } yield {
      MonthlyByDayOfMonth(getId, frequency, timesHasRun, totalTimesToRun, dayOfMonth, localTime, zoneId)
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
      MonthlyByNthDayOfWeek(getId, frequency, timesHasRun, totalTimesToRun, dayOfWeek, nthDayOfWeek, localTime, zoneId)
    }
  }

  def maybeToYearly: Option[Yearly] = {
    for {
      _ <- Option(typeName).filter(_ == Yearly.recurrenceType)
      monthDay <- maybeValidMonthDay
      localTime <- maybeLocalTime
      zoneId <- maybeZoneId
    } yield {
      Yearly(getId, frequency, timesHasRun, totalTimesToRun, monthDay, localTime, zoneId)
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
      Some(recurrence.id),
      recurrence.displayString,
      recurrence.frequency,
      recurrence.timesHasRun,
      recurrence.maybeTotalTimesToRun,
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
