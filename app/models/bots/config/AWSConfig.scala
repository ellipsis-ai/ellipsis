package models.bots.config

import models.IDs
import models.bots.behaviorversion.BehaviorVersion
import models.environmentvariable.EnvironmentVariable
import services.DataService
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global

case class AWSConfig(
                    id: String,
                    behaviorVersionId: String,
                    maybeAccessKey: Option[EnvironmentVariable],
                    maybeSecretKey: Option[EnvironmentVariable],
                    maybeRegion: Option[EnvironmentVariable]
                      ) {
  def maybeAccessKeyName = maybeAccessKey.map(_.name)
  def maybeSecretKeyName = maybeSecretKey.map(_.name)
  def maybeRegionName = maybeRegion.map(_.name)

  def environmentVariableNames: Seq[String] = Seq(maybeAccessKeyName, maybeSecretKeyName, maybeRegionName).flatten
}

case class RawAWSConfig(
                       id: String,
                       behaviorVersionId: String,
                       maybeAccessKeyName: Option[String],
                       maybeSecretKeyName: Option[String],
                       maybeRegionName: Option[String]
                         )

class AWSConfigsTable(tag: Tag) extends Table[RawAWSConfig](tag, "aws_configs") {

  def id = column[String]("id", O.PrimaryKey)
  def behaviorVersionId = column[String]("behavior_version_id")
  def maybeAccessKeyName = column[Option[String]]("access_key_name")
  def maybeSecretKeyName = column[Option[String]]("secret_key_name")
  def maybeRegionName = column[Option[String]]("region_name")

  def * = (id, behaviorVersionId, maybeAccessKeyName, maybeSecretKeyName, maybeRegionName) <> ((RawAWSConfig.apply _).tupled, RawAWSConfig.unapply _)
}

object AWSConfigQueries {
  val all = TableQuery[AWSConfigsTable]

  def uncompiledFindQuery(behaviorVersionId: Rep[String]) = all.filter(_.behaviorVersionId === behaviorVersionId)
  val findQuery = Compiled(uncompiledFindQuery _)

  def maybeFor(behaviorVersion: BehaviorVersion, dataService: DataService): DBIO[Option[AWSConfig]] = {
    for {
      maybeRaw <- findQuery(behaviorVersion.id).result.map(_.headOption)
      maybeAccessKey <- maybeRaw.flatMap { raw =>
        raw.maybeAccessKeyName.map { accessKeyName =>
          DBIO.from(dataService.environmentVariables.find(accessKeyName, behaviorVersion.team))
        }
      }.getOrElse(DBIO.successful(None))
      maybeSecretKey <- maybeRaw.flatMap { raw =>
        raw.maybeSecretKeyName.map { secretKeyName =>
          DBIO.from(dataService.environmentVariables.find(secretKeyName, behaviorVersion.team))
        }
      }.getOrElse(DBIO.successful(None))
      maybeRegion <- maybeRaw.flatMap { raw =>
        raw.maybeRegionName.map { regionName =>
          DBIO.from(dataService.environmentVariables.find(regionName, behaviorVersion.team))
        }
      }.getOrElse(DBIO.successful(None))
    } yield {
      maybeRaw.map { raw =>
        AWSConfig(raw.id, raw.behaviorVersionId, maybeAccessKey, maybeSecretKey, maybeRegion)
      }
    }

  }

  def createFor(
                 behaviorVersion: BehaviorVersion,
                 maybeAccessKeyName: Option[String],
                 maybeSecretKeyName: Option[String],
                 maybeRegionName: Option[String],
                 dataService: DataService
                 ): DBIO[AWSConfig] = {
    for {
      maybeAccessKey <- maybeAccessKeyName.map { name =>
        DBIO.from(dataService.environmentVariables.ensureFor(name, None, behaviorVersion.team))
      }.getOrElse(DBIO.successful(None))
      maybeSecretKey <- maybeSecretKeyName.map { name =>
        DBIO.from(dataService.environmentVariables.ensureFor(name, None, behaviorVersion.team))
      }.getOrElse(DBIO.successful(None))
      maybeRegion <- maybeRegionName.map { name =>
        DBIO.from(dataService.environmentVariables.ensureFor(name, None, behaviorVersion.team))
      }.getOrElse(DBIO.successful(None))
      newInstance <- {
        val raw =
          RawAWSConfig(
            IDs.next,
            behaviorVersion.id,
            maybeAccessKey.map(_.name),
            maybeSecretKey.map(_.name),
            maybeRegion.map(_.name)
          )
        (all += raw).map { _ => AWSConfig(raw.id, raw.behaviorVersionId, maybeAccessKey, maybeSecretKey, maybeRegion) }
      }
    } yield newInstance
  }

}
