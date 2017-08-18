package models.behaviors.config.awsconfig

import javax.inject.Inject

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import models.IDs
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AWSConfigsTable(tag: Tag) extends Table[AWSConfig](tag, "aws_configs") {

  def id = column[String]("id", O.PrimaryKey)
  def groupVersionId = column[String]("group_version_id")
  def maybeAccessKeyName = column[Option[String]]("access_key_name")
  def maybeSecretKeyName = column[Option[String]]("secret_key_name")
  def maybeRegionName = column[Option[String]]("region_name")

  def * = (id, groupVersionId, maybeAccessKeyName, maybeSecretKeyName, maybeRegionName) <> ((AWSConfig.apply _).tupled, AWSConfig.unapply _)
}

class AWSConfigServiceImpl @Inject() (
                                       dataServiceProvider: Provider[DataService]
                                     ) extends AWSConfigService {

  def dataService = dataServiceProvider.get

  val all = TableQuery[AWSConfigsTable]

  def uncompiledFindQuery(behaviorVersionId: Rep[String]) = all.filter(_.groupVersionId === behaviorVersionId)
  val findQuery = Compiled(uncompiledFindQuery _)

  def maybeForAction(groupVersion: BehaviorGroupVersion): DBIO[Option[AWSConfig]] = {
    findQuery(groupVersion.id).result.map(_.headOption)
  }

  def maybeFor(groupVersion: BehaviorGroupVersion): Future[Option[AWSConfig]] = {
    dataService.run(maybeForAction(groupVersion))
  }

  def createForAction(
                       groupVersion: BehaviorGroupVersion,
                       maybeAccessKeyName: Option[String],
                       maybeSecretKeyName: Option[String],
                       maybeRegionName: Option[String]
                     ): DBIO[AWSConfig] = {

    val newInstance = AWSConfig(
      IDs.next,
      groupVersion.id,
      maybeAccessKeyName,
      maybeSecretKeyName,
      maybeRegionName
    )

    (all += newInstance).map { _ => newInstance }
  }

}
