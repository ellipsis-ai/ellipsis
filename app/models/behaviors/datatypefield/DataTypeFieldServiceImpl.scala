package models.behaviors.datatypefield

import javax.inject.Inject

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import models.behaviors.datatypeconfig.DataTypeConfig
import models.behaviors.defaultstorageitem.{DefaultStorageItem, DefaultStorageItemService}
import sangria.schema._
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RawDataTypeField(id: String, name: String, fieldTypeId: String, configId: String)

class DataTypeFieldsTable(tag: Tag) extends Table[RawDataTypeField](tag, "data_type_fields") {

  def id = column[String]("id", O.PrimaryKey)
  def name = column[String]("name")
  def fieldTypeId = column[String]("field_type")
  def configId = column[String]("config_id")

  def * =
    (id, name, fieldTypeId, configId) <> ((RawDataTypeField.apply _).tupled, RawDataTypeField.unapply _)
}

class DataTypeFieldServiceImpl @Inject() (
                                           dataServiceProvider: Provider[DataService]
                                         ) extends DataTypeFieldService {

  def dataService = dataServiceProvider.get

  import DataTypeFieldQueries._

  def graphQLFor(
                  field: DataTypeField,
                  seen: scala.collection.mutable.Map[DataTypeConfig, ObjectType[DefaultStorageItemService, DefaultStorageItem]]
                ): Future[Field[DefaultStorageItemService, DefaultStorageItem]] = {
    import models.behaviors.behaviorparameter._
    val eventualFieldType = field.fieldType match {
      case TextType => Future.successful(StringType)
      case NumberType => Future.successful(FloatType)
      case YesNoType => Future.successful(BooleanType)
      case t: BehaviorBackedDataType => dataService.dataTypeConfigs.graphQLTypeFor(t.dataTypeConfig, seen)
    }
    eventualFieldType.map { ft =>
      Field(field.name, ft, resolve = c => (c.value.data \ field.name).toOption)
    }
//    field.fieldType.graphQLType(dataService, seen).map { graphQLFieldType =>
//      Field(field.name, graphQLFieldType, resolve = c => (c.value.data \ field.name).toOption)
//    }

  }

  def allFor(config: DataTypeConfig): Future[Seq[DataTypeField]] = {
    val action = allForConfigQuery(config.id).result.map { r =>
      r.map(tuple2Field)
    }
    dataService.run(action)
  }

}
