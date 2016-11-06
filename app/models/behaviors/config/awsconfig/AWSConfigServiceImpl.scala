package models.behaviors.config.awsconfig

import javax.inject.Inject

import com.google.inject.Provider
import models.IDs
import models.behaviors.behaviorversion.BehaviorVersion
import services.DataService
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AWSConfigsTable(tag: Tag) extends Table[AWSConfig](tag, "aws_configs") {

  def id = column[String]("id", O.PrimaryKey)
  def behaviorVersionId = column[String]("behavior_version_id")
  def maybeAccessKeyName = column[Option[String]]("access_key_name")
  def maybeSecretKeyName = column[Option[String]]("secret_key_name")
  def maybeRegionName = column[Option[String]]("region_name")

  def * = (id, behaviorVersionId, maybeAccessKeyName, maybeSecretKeyName, maybeRegionName) <> ((AWSConfig.apply _).tupled, AWSConfig.unapply _)
}

class AWSConfigServiceImpl @Inject() (
                                       dataServiceProvider: Provider[DataService]
                                     ) extends AWSConfigService {

  def dataService = dataServiceProvider.get

  val all = TableQuery[AWSConfigsTable]

  def uncompiledFindQuery(behaviorVersionId: Rep[String]) = all.filter(_.behaviorVersionId === behaviorVersionId)
  val findQuery = Compiled(uncompiledFindQuery _)

  def maybeFor(behaviorVersion: BehaviorVersion): Future[Option[AWSConfig]] = {
    val action = findQuery(behaviorVersion.id).result.map(_.headOption)
    dataService.run(action)
  }

  def createFor(
                 behaviorVersion: BehaviorVersion,
                 maybeAccessKeyName: Option[String],
                 maybeSecretKeyName: Option[String],
                 maybeRegionName: Option[String]
               ): Future[AWSConfig] = {

    val newInstance = AWSConfig(
      IDs.next,
      behaviorVersion.id,
      maybeAccessKeyName,
      maybeSecretKeyName,
      maybeRegionName
    )

    val action = (all += newInstance).map { _ => newInstance }
    dataService.run(action)
  }

}
