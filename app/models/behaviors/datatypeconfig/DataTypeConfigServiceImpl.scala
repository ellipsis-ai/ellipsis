package models.behaviors.datatypeconfig

import javax.inject.Inject

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import json.DataTypeConfigData
import models.IDs
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.behaviorversion.BehaviorVersion
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

case class RawDataTypeConfig(
                              id: String,
                              maybeUsesCode: Option[Boolean],
                              behaviorVersionId: String
                            )

class DataTypeConfigsTable(tag: Tag) extends Table[RawDataTypeConfig](tag, "data_type_configs") {

  def id = column[String]("id", O.PrimaryKey)
  def maybeUsesCode = column[Option[Boolean]]("uses_code")
  def behaviorVersionId = column[String]("behavior_version_id")

  def * =
    (id, maybeUsesCode, behaviorVersionId) <> ((RawDataTypeConfig.apply _).tupled, RawDataTypeConfig.unapply _)
}

class DataTypeConfigServiceImpl @Inject() (
                                             dataServiceProvider: Provider[DataService],
                                             implicit val ec: ExecutionContext
                                           ) extends DataTypeConfigService {

  def dataService = dataServiceProvider.get

  import DataTypeConfigQueries._

  def allForAction(groupVersion: BehaviorGroupVersion): DBIO[Seq[DataTypeConfig]] = {
    allForQuery(groupVersion.id).result.map { r =>
      r.map(tuple2Config)
    }
  }

  def allFor(groupVersion: BehaviorGroupVersion): Future[Seq[DataTypeConfig]] = {
    dataService.run(allForAction(groupVersion))
  }

  def allUsingDefaultStorageFor(groupVersion: BehaviorGroupVersion): Future[Seq[DataTypeConfig]] = {
    val action = allUsingDefaultStorageForQuery(groupVersion.id).result.map { r =>
      r.map(tuple2Config)
    }
    dataService.run(action)
  }

  def maybeForAction(behaviorVersion: BehaviorVersion): DBIO[Option[DataTypeConfig]] = {
    maybeForQuery(behaviorVersion.id).result.map { r =>
      r.headOption.map(tuple2Config)
    }
  }

  def maybeFor(behaviorVersion: BehaviorVersion): Future[Option[DataTypeConfig]] = {
    dataService.run(maybeForAction(behaviorVersion))
  }

  def findAction(id: String): DBIO[Option[DataTypeConfig]] = {
    findQuery(id).result.map { r =>
      r.headOption.map(tuple2Config)
    }
  }

  def find(id: String): Future[Option[DataTypeConfig]] = {
    dataService.run(findAction(id))
  }

  def createForAction(behaviorVersion: BehaviorVersion, data: DataTypeConfigData): DBIO[DataTypeConfig] = {
    val newInstance = DataTypeConfig(IDs.next, data.usesCode, behaviorVersion)
    (all += newInstance.toRaw).map(_ => newInstance)
  }


}
