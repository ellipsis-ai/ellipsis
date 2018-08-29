package models.behaviors.scheduling.recurrence

import java.time.{DayOfWeek, LocalTime, ZoneId}
import javax.inject.Inject

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

case class RawRecurrence(
                          id: String,
                          recurrenceType: String,
                          frequency: Int,
                          timesHasRun: Int,
                          maybeTotalTimesToRun: Option[Int],
                          maybeTimeOfDay: Option[LocalTime],
                          maybeTimeZone: Option[ZoneId],
                          maybeMinuteOfHour: Option[Int],
                          maybeDayOfWeekNumber: Option[Int],
                          maybeMonday: Option[Boolean],
                          maybeTuesday: Option[Boolean],
                          maybeWednesday: Option[Boolean],
                          maybeThursday: Option[Boolean],
                          maybeFriday: Option[Boolean],
                          maybeSaturday: Option[Boolean],
                          maybeSunday: Option[Boolean],
                          maybeDayOfMonth: Option[Int],
                          maybeNthDayOfWeek: Option[Int],
                          maybeMonth: Option[Int]
                        ) {

  val maybeDayOfWeek = maybeDayOfWeekNumber.map(DayOfWeek.of)

  val daysOfWeek: Seq[DayOfWeek] = {
    DayOfWeek.values.zip(
      Seq(
        maybeMonday,
        maybeTuesday,
        maybeWednesday,
        maybeThursday,
        maybeFriday,
        maybeSaturday,
        maybeSunday
      )
    ).
      filter { case(day, maybeOn) => maybeOn.exists(identity) }.
      map { case(day, _) => day }
  }

}

class RecurrencesTable(tag: Tag) extends Table[RawRecurrence](tag, "recurrences") {

  def id = column[String]("id")
  def recurrenceType = column[String]("recurrence_type")
  def frequency = column[Int]("frequency")
  def maybeTimeOfDay = column[Option[LocalTime]]("time_of_day")
  def maybeTimeZone = column[Option[ZoneId]]("time_zone")
  def maybeMinuteOfHour = column[Option[Int]]("minute_of_hour")
  def maybeDayOfWeek = column[Option[Int]]("day_of_week")
  def maybeMonday = column[Option[Boolean]]("monday")
  def maybeTuesday = column[Option[Boolean]]("tuesday")
  def maybeWednesday = column[Option[Boolean]]("wednesday")
  def maybeThursday = column[Option[Boolean]]("thursday")
  def maybeFriday = column[Option[Boolean]]("friday")
  def maybeSaturday = column[Option[Boolean]]("saturday")
  def maybeSunday = column[Option[Boolean]]("sunday")
  def maybeDayOfMonth = column[Option[Int]]("day_of_month")
  def maybeNthDayOfWeek = column[Option[Int]]("nth_day_of_week")
  def maybeMonth = column[Option[Int]]("month")
  def maybeTotalTimesToRun = column[Option[Int]]("total_times_to_run")
  def timesHasRun = column[Int]("times_has_run")

  def * = (
    id,
    recurrenceType,
    frequency,
    timesHasRun,
    maybeTotalTimesToRun,
    maybeTimeOfDay,
    maybeTimeZone,
    maybeMinuteOfHour,
    maybeDayOfWeek,
    maybeMonday,
    maybeTuesday,
    maybeWednesday,
    maybeThursday,
    maybeFriday,
    maybeSaturday,
    maybeSunday,
    maybeDayOfMonth,
    maybeNthDayOfWeek,
    maybeMonth
  ) <> ((RawRecurrence.apply _).tupled, RawRecurrence.unapply _)

}

class RecurrenceServiceImpl @Inject() (
                                        dataServiceProvider: Provider[DataService],
                                        implicit val ec: ExecutionContext
                                      ) extends RecurrenceService {

  def dataService = dataServiceProvider.get

  import RecurrenceQueries._

  /* TODO: Investigate why saving time-of-day midnight causes a PostgreSQL error

     Midnight causes a mapping error with an invalid `-infinity` value,
     so we ensure time of day is at least one nanosecond later.

     (PostgreSQL throws away nanoseconds anyway.) */
  private def ensureAfterMinTimeOfDay(raw: RawRecurrence): RawRecurrence = {
    raw.copy(maybeTimeOfDay = raw.maybeTimeOfDay.map(_.plusNanos(1)))
  }

  def saveAction(recurrence: Recurrence): DBIO[Recurrence] = {
    val newRawRecurrence = ensureAfterMinTimeOfDay(recurrence.toRaw)
    val query = all.filter(_.id === newRawRecurrence.id)
    query.result.flatMap { r =>
      r.headOption.map { existing =>
        query.update(newRawRecurrence)
      }.getOrElse {
        all += newRawRecurrence
      }
    }.map { _ => recurrence }
  }

  def save(recurrence: Recurrence): Future[Recurrence] = {
    dataService.run(saveAction(recurrence))
  }

  def maybeCreateFromText(text: String, defaultTimeZone: ZoneId): Future[Option[Recurrence]] = {
    Recurrence.maybeUnsavedFromText(text, defaultTimeZone).map { newRecurrence =>
      save(newRecurrence).map(Some(_))
    }.getOrElse(Future.successful(None))
  }

  def deleteAction(recurrenceId: String): DBIO[Boolean] = {
    findRawQuery(recurrenceId).delete.map(r => r > 0)
  }

  def delete(recurrenceId: String): Future[Boolean] = {
    dataService.run(deleteAction(recurrenceId))
  }

}
