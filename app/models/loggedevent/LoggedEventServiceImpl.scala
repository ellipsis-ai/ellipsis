package models.loggedevent

import java.time.OffsetDateTime
import javax.inject.Inject

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import play.api.libs.json.JsValue
import services.DataService
import slick.ast.ColumnOption.PrimaryKey

import scala.concurrent.{ExecutionContext, Future}

class LoggedEventsTable(tag: Tag) extends Table[LoggedEvent](tag, "logged_events") {

  implicit val eventTypeColumnType = MappedColumnType.base[LoggedEventType, String](
    { gt => gt.toString },
    { str => LoggedEventType.definitelyFind(str) }
  )

  def id = column[String]("id", PrimaryKey)
  def eventType = column[LoggedEventType]("type")
  def maybeUserId = column[Option[String]]("user_id")
  def maybeMedium = column[Option[String]]("medium")
  def maybeChannel = column[Option[String]]("channel")
  def details = column[JsValue]("details")
  def createdAt = column[OffsetDateTime]("created_at")

  def * = (id, eventType, maybeUserId, maybeMedium, maybeChannel, details, createdAt) <> ((LoggedEvent.apply _).tupled, LoggedEvent.unapply _)
}

class LoggedEventServiceImpl @Inject() (
                                            dataServiceProvider: Provider[DataService],
                                            implicit val ec: ExecutionContext
                                          ) extends LoggedEventService {

  def dataService = dataServiceProvider.get

  import LoggedEventQueries._

  def log(event: LoggedEvent): Future[Unit] = {
    val action = (all += event).map(_ => {})
    dataService.run(action)
  }

}
