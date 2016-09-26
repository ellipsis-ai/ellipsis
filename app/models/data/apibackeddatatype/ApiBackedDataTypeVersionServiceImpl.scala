package models.data.apibackeddatatype

import javax.inject.Inject

import com.github.tototoshi.slick.PostgresJodaSupport._
import com.google.inject.Provider
import org.joda.time.DateTime
import services.DataService
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RawApiBackedDataTypeVersion(
                                        id: String,
                                        name: String,
                                        dataTypeId: String,
                                        maybeHttpMethod: Option[String],
                                        maybeUrl: Option[String],
                                        maybeRequestBody: Option[String],
                                        maybeFunctionBody: Option[String],
                                        createdAt: DateTime
                               )

class ApiBackedDataTypeVersionsTable(tag: Tag) extends Table[RawApiBackedDataTypeVersion](tag, "api_backed_data_type_versions") {

  def id = column[String]("id", O.PrimaryKey)
  def name = column[String]("name")
  def dataTypeId = column[String]("data_type_id")
  def maybeHttpMethod = column[Option[String]]("http_method")
  def maybeUrl = column[Option[String]]("url")
  def maybeRequestBody = column[Option[String]]("request_body")
  def maybeFunctionBody = column[Option[String]]("function_body")
  def createdAt = column[DateTime]("created_at")

  def * = (id, name, dataTypeId, maybeHttpMethod, maybeUrl, maybeRequestBody, maybeFunctionBody, createdAt) <>
    ((RawApiBackedDataTypeVersion.apply _).tupled, RawApiBackedDataTypeVersion.unapply _)
}

class ApiBackedDataTypeVersionServiceImpl @Inject() (
                                               dataServiceProvider: Provider[DataService]
                                             ) extends ApiBackedDataTypeVersionService {

  def dataService = dataServiceProvider.get

  import ApiBackedDataTypeVersionQueries._

  def uncompiledFindQuery(id: Rep[String]) = joined.filter { case(version, (dataType, team)) => version.id === id }
  val findQuery = Compiled(uncompiledFindQuery _)

  def find(id: String): Future[Option[ApiBackedDataTypeVersion]] = {
    val action = findQuery(id).result.map { r =>
      r.headOption.map(tuple2DataType)
    }
    dataService.run(action)
  }

}
