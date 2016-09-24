package models.data.apibackeddatatype

import javax.inject.Inject

import com.github.tototoshi.slick.PostgresJodaSupport._
import com.google.inject.Provider
import models.IDs
import models.accounts.user.User
import models.team.{Team, TeamQueries}
import org.joda.time.DateTime
import play.api.libs.json.{JsValue, Json}
import services.DataService
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RawApiBackedDataTypeVersion(
                                        id: String,
                                        dataTypeId: String,
                                        maybeHttpMethod: Option[String],
                                        maybeUrl: Option[String],
                                        maybeRequestBody: Option[String],
                                        maybeFunctionBody: Option[String],
                                        createdAt: DateTime
                               )

class ApiBackedDataTypeVersionsTable(tag: Tag) extends Table[RawApiBackedDataTypeVersion](tag, "api_backed_data_type_versions") {

  def id = column[String]("id", O.PrimaryKey)
  def dataTypeId = column[String]("data_type_id")
  def maybeHttpMethod = column[Option[String]]("http_method")
  def maybeUrl = column[Option[String]]("url")
  def maybeRequestBody = column[Option[String]]("request_body")
  def maybeFunctionBody = column[Option[String]]("function_body")
  def createdAt = column[DateTime]("created_at")

  def * = (id, dataTypeId, maybeHttpMethod, maybeUrl, maybeRequestBody, maybeFunctionBody, createdAt) <>
    ((RawApiBackedDataTypeVersion.apply _).tupled, RawApiBackedDataTypeVersion.unapply _)
}

class ApiBackedDataTypeVersionServiceImpl @Inject() (
                                               dataServiceProvider: Provider[DataService]
                                             ) extends ApiBackedDataTypeVersionService {

  def dataService = dataServiceProvider.get

  import ApiBackedDataTypeVersionQueries._

//  def uncompiledAllForQuery(teamId: Rep[String]) = joined.filter(_._1.teamId === teamId)
//  val allForQuery = Compiled(uncompiledAllForQuery _)
//
//  def allFor(team: Team): Future[Seq[ApiBackedDataType]] = {
//    val action = allForQuery(team.id).result.map { r =>
//      r.map(tuple2DataType)
//    }
//    dataService.run(action)
//  }

}
