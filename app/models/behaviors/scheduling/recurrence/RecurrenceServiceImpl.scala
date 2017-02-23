package models.behaviors.scheduling.recurrence

import java.time.{DayOfWeek, LocalTime, ZoneId}
import javax.inject.Inject

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RawRecurrence(
                          id: String,
                          recurrenceType: String,
                          frequency: Int,
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

  def * = (
    id,
    recurrenceType,
    frequency,
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
                                        dataServiceProvider: Provider[DataService]
                                      ) extends RecurrenceService {

  def dataService = dataServiceProvider.get

  import RecurrenceQueries._

  def save(recurrence: Recurrence): Future[Recurrence] = {
    val raw = recurrence.toRaw
    val query = all.filter(_.id === raw.id)
    val action = query.result.flatMap { r =>
      r.headOption.map { existing =>
        query.update(raw)
      }.getOrElse {
        all += raw
      }
    }.map { _ => recurrence }
    dataService.run(action)
  }

  def maybeCreateFromText(text: String, defaultTimeZone: ZoneId): Future[Option[Recurrence]] = {
    Recurrence.maybeUnsavedFromText(text, defaultTimeZone).map { newRecurrence =>
      save(newRecurrence).map(Some(_))
    }.getOrElse(Future.successful(None))
  }

  def delete(recurrenceId: String): Future[Boolean] = {
    val action = findRawQuery(recurrenceId).delete.map(r => r > 0)
    dataService.run(action)
  }

}
