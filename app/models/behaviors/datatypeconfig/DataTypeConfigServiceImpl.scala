package models.behaviors.datatypeconfig

import javax.inject.Inject

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import models.IDs
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.defaultstorageitem.{DefaultStorageItem, DefaultStorageItemService}
import sangria.schema._
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RawDataTypeConfig(id: String, behaviorVersionId: String)

class DataTypeConfigsTable(tag: Tag) extends Table[RawDataTypeConfig](tag, "data_type_configs") {

  def id = column[String]("id", O.PrimaryKey)
  def behaviorVersionId = column[String]("behavior_version_id")

  def * =
    (id, behaviorVersionId) <> ((RawDataTypeConfig.apply _).tupled, RawDataTypeConfig.unapply _)
}

class DataTypeConfigServiceImpl @Inject() (
                                             dataServiceProvider: Provider[DataService]
                                           ) extends DataTypeConfigService {

  def dataService = dataServiceProvider.get

  import DataTypeConfigQueries._

  def graphQLTypeFor(
                      config: DataTypeConfig,
                      seen: scala.collection.mutable.Map[DataTypeConfig, ObjectType[DefaultStorageItemService, DefaultStorageItem]]
                    ): Future[ObjectType[DefaultStorageItemService, DefaultStorageItem]] = {
    seen.get(config).map { existingType =>
      Future.successful(existingType)
    }.getOrElse {
      dataService.dataTypeFields.allFor(config).flatMap { dataTypeFields =>
        Future.sequence(dataTypeFields.map { ea =>
          dataService.dataTypeFields.graphQLFor(ea, seen)
        }).map { fields =>
          val newType = ObjectType(
            config.name,
            config.behaviorVersion.description,
            fields.toList
          )
          seen.put(config, newType)
          newType
        }
      }
    }

  }

  def allForAction(groupVersion: BehaviorGroupVersion): DBIO[Seq[DataTypeConfig]] = {
    allForQuery(groupVersion.id).result.map { r =>
      r.map(tuple2Config)
    }
  }

  def allFor(groupVersion: BehaviorGroupVersion): Future[Seq[DataTypeConfig]] = {
    dataService.run(allForAction(groupVersion))
  }

  def createForAction(behaviorVersion: BehaviorVersion): DBIO[DataTypeConfig] = {
    val newInstance = DataTypeConfig(IDs.next, behaviorVersion)
    (all += newInstance.toRaw).map(_ => newInstance)
  }


}
