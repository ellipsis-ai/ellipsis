package models.behaviors.config.awsconfig

import javax.inject.Inject

import com.google.inject.Provider
import models.IDs
import models.behaviors.behaviorversion.BehaviorVersion
import services.DataService
import drivers.SlickPostgresDriver.api._

import scala.concurrent.{ExecutionContext, Future}

class AWSConfigsTable(tag: Tag) extends Table[AWSConfig](tag, "aws_configs") {

  def id = column[String]("id", O.PrimaryKey)
  def behaviorVersionId = column[String]("behavior_version_id")
  def maybeAccessKeyName = column[Option[String]]("access_key_name")
  def maybeSecretKeyName = column[Option[String]]("secret_key_name")
  def maybeRegionName = column[Option[String]]("region_name")

  def * = (id, behaviorVersionId, maybeAccessKeyName, maybeSecretKeyName, maybeRegionName) <> ((AWSConfig.apply _).tupled, AWSConfig.unapply _)
}

class AWSConfigServiceImpl @Inject() (
                                       dataServiceProvider: Provider[DataService],
                                       implicit val ec: ExecutionContext
                                     ) extends AWSConfigService {

  def dataService = dataServiceProvider.get

  val all = TableQuery[AWSConfigsTable]

  def uncompiledFindQuery(behaviorVersionId: Rep[String]) = all.filter(_.behaviorVersionId === behaviorVersionId)
  val findQuery = Compiled(uncompiledFindQuery _)

  def maybeForAction(behaviorVersion: BehaviorVersion): DBIO[Option[AWSConfig]] = {
    findQuery(behaviorVersion.id).result.map(_.headOption)
  }

  def maybeFor(behaviorVersion: BehaviorVersion): Future[Option[AWSConfig]] = {
    dataService.run(maybeForAction(behaviorVersion))
  }

  def createForAction(
                       behaviorVersion: BehaviorVersion,
                       maybeAccessKeyName: Option[String],
                       maybeSecretKeyName: Option[String],
                       maybeRegionName: Option[String]
                     ): DBIO[AWSConfig] = {

    val newInstance = AWSConfig(
      IDs.next,
      behaviorVersion.id,
      maybeAccessKeyName,
      maybeSecretKeyName,
      maybeRegionName
    )

    (all += newInstance).map { _ => newInstance }
  }

}
