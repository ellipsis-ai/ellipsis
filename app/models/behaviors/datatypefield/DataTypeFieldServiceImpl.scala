package models.behaviors.datatypefield

import javax.inject.Inject

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import json.DataTypeFieldData
import models.IDs
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.behaviorparameter.{BehaviorParameterType, TextType}
import models.behaviors.datatypeconfig.DataTypeConfig
import services.DataService
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RawDataTypeField(id: String, fieldId: String, name: String, fieldTypeId: String, configId: String, rank: Int)

class DataTypeFieldsTable(tag: Tag) extends Table[RawDataTypeField](tag, "data_type_fields") {

  def id = column[String]("id", O.PrimaryKey)
  def fieldId = column[String]("field_id")
  def name = column[String]("name")
  def fieldTypeId = column[String]("field_type")
  def configId = column[String]("config_id")
  def rank = column[Int]("rank")

  def * =
    (id, fieldId, name, fieldTypeId, configId, rank) <> ((RawDataTypeField.apply _).tupled, RawDataTypeField.unapply _)
}

class DataTypeFieldServiceImpl @Inject() (
                                           dataServiceProvider: Provider[DataService]
                                         ) extends DataTypeFieldService {

  def dataService = dataServiceProvider.get

  import DataTypeFieldQueries._

  def builtInFieldsFor(config: DataTypeConfig): Seq[DataTypeField] = {
    Seq(DataTypeField("id", "id", "id", TextType, config.id, 0))
  }

  def allForAction(config: DataTypeConfig): DBIO[Seq[DataTypeField]] = {
    allForConfigQuery(config.id).result.map { r =>
      builtInFieldsFor(config) ++ r.map(tuple2Field)
    }
  }

  def allFor(config: DataTypeConfig): Future[Seq[DataTypeField]] = {
    dataService.run(allForAction(config))
  }

  private def maybeParamTypeForAction(data: DataTypeFieldData, behaviorGroupVersion: BehaviorGroupVersion): DBIO[Option[BehaviorParameterType]] = {
    (data.fieldType.flatMap { paramTypeData =>
      paramTypeData.id.orElse(paramTypeData.exportId).map { id =>
        BehaviorParameterType.findAction(id, behaviorGroupVersion, dataService)
      }
    }.getOrElse(DBIO.successful(None)))
  }

  def createForAction(data: DataTypeFieldData, rank: Int, config: DataTypeConfig, behaviorGroupVersion: BehaviorGroupVersion): DBIO[DataTypeField] = {
    maybeParamTypeForAction(data, behaviorGroupVersion).flatMap { fieldType =>
      val newInstance =
        DataTypeField(
          data.id.getOrElse(IDs.next),
          data.fieldId.getOrElse(IDs.next),
          data.name,
          fieldType.getOrElse(TextType),
          config.id,
          rank
        )
      (all += newInstance.toRaw).map { _ => newInstance }
    }
  }

}
