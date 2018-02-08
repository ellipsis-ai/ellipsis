package models.loggedevent

import java.time.OffsetDateTime
import javax.inject.Inject

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import play.api.libs.json.{JsValue, Json}
import services.DataService
import slick.ast.ColumnOption.PrimaryKey
import slick.dbio.DBIO

import scala.concurrent.{ExecutionContext, Future}

class LoggedEventsTable(tag: Tag) extends Table[LoggedEvent](tag, "logged_events") {

  import Formatting._

  implicit val causeTypeColumnType = MappedColumnType.base[CauseType, String](
    { v => v.toString },
    { str => CauseType.definitelyFind(str) }
  )

  implicit val causeDetailsColumnType =  MappedColumnType.base[CauseDetails, JsValue](
    { v => Json.toJson(v) },
    { json => CauseDetails.fromJson(json) }
  )

  implicit val resultTypeColumnType = MappedColumnType.base[ResultType, String](
    { gt => gt.toString },
    { str => ResultType.definitelyFind(str) }
  )

  implicit val resultDetailsColumnType =  MappedColumnType.base[ResultDetails, JsValue](
    { v => Json.toJson(v) },
    { json => ResultDetails.fromJson(json) }
  )

  def id = column[String]("id", PrimaryKey)
  def causeType = column[CauseType]("cause_type")
  def causeDetails = column[CauseDetails]("cause_details")
  def resultType = column[ResultType]("result_type")
  def resultDetails = column[ResultDetails]("result_details")
  def maybeUserId = column[Option[String]]("user_id")
  def createdAt = column[OffsetDateTime]("created_at")

  def * = (id, causeType, causeDetails, resultType, resultDetails, maybeUserId, createdAt) <> ((LoggedEvent.apply _).tupled, LoggedEvent.unapply _)
}

class LoggedEventServiceImpl @Inject() (
                                            dataServiceProvider: Provider[DataService],
                                            implicit val ec: ExecutionContext
                                          ) extends LoggedEventService {

  def dataService = dataServiceProvider.get

  import LoggedEventQueries._

  def logAction(event: LoggedEvent): DBIO[Unit] = {
    (all += event).map(_ => {})
  }

  def log(event: LoggedEvent): Future[Unit] = {
    dataService.run(logAction(event))
  }

}
