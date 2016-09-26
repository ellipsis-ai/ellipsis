package models.data.apibackeddatatype

import javax.inject.Inject

import com.github.tototoshi.slick.PostgresJodaSupport._
import com.google.inject.Provider
import models.IDs
import models.accounts.user.User
import models.team.{Team, TeamQueries}
import org.joda.time.DateTime
import services.DataService
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RawApiBackedDataType(
                              id: String,
                              teamId: String,
                              maybeCurrentVersionId: Option[String],
                              maybeImportedId: Option[String],
                              createdAt: DateTime
                            )

class ApiBackedDataTypesTable(tag: Tag) extends Table[RawApiBackedDataType](tag, "api_backed_data_types") {

  def id = column[String]("id", O.PrimaryKey)
  def teamId = column[String]("team_id")
  def maybeCurrentVersionId = column[Option[String]]("current_version_id")
  def maybeImportedId = column[Option[String]]("imported_id")
  def createdAt = column[DateTime]("created_at")

  def * = (id, teamId, maybeCurrentVersionId, maybeImportedId, createdAt) <>
    ((RawApiBackedDataType.apply _).tupled, RawApiBackedDataType.unapply _)
}

class ApiBackedDataTypeServiceImpl @Inject() (
                                              dataServiceProvider: Provider[DataService]
                                            ) extends ApiBackedDataTypeService {

  def dataService = dataServiceProvider.get

  import ApiBackedDataTypeQueries._

  def uncompiledAllForQuery(teamId: Rep[String]) = joined.filter(_._1.teamId === teamId)
  val allForQuery = Compiled(uncompiledAllForQuery _)

  def allFor(team: Team): Future[Seq[ApiBackedDataType]] = {
    val action = allForQuery(team.id).result.map { r =>
      r.map(tuple2DataType)
    }
    dataService.run(action)
  }

}
