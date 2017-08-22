package models.behaviors.config.awsconfig

import javax.inject.Inject

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import models.team.Team
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AWSConfigsTable(tag: Tag) extends Table[AWSConfig](tag, "aws_configs") {

  def id = column[String]("id", O.PrimaryKey)
  def name = column[String]("name")
  def teamId = column[String]("team_id")
  def maybeAccessKeyId = column[Option[String]]("access_key_id")
  def maybeSecretAccessKey = column[Option[String]]("secret_access_key")
  def maybeRegion = column[Option[String]]("region")

  def * = (id, name, teamId, maybeAccessKeyId, maybeSecretAccessKey, maybeRegion) <> ((AWSConfig.apply _).tupled, AWSConfig.unapply _)
}

class AWSConfigServiceImpl @Inject() (
                                       dataServiceProvider: Provider[DataService]
                                     ) extends AWSConfigService {

  def dataService = dataServiceProvider.get

  import AWSConfigQueries._

  def allFor(team: Team): Future[Seq[AWSConfig]] = {
    val action = allForQuery(team.id).result
    dataService.run(action)
  }

  def findAction(id: String): DBIO[Option[AWSConfig]] = {
    findQuery(id).result.map { r =>
      r.headOption
    }
  }

  def find(id: String): Future[Option[AWSConfig]] = {
    dataService.run(findAction(id))
  }

  def save(config: AWSConfig): Future[AWSConfig] = {
    find(config.id).flatMap { maybeExisting =>
      maybeExisting.map { existing =>
        dataService.run(findQuery(config.id).update(config))
      }.getOrElse {
        val action = (all += config)
        dataService.run(action)
      }
    }.map { _ => config }
  }

}
