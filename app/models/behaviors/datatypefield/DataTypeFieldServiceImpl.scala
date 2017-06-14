package models.behaviors.datatypefield

import javax.inject.Inject

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import models.IDs
import models.behaviors.behaviorparameter.BehaviorParameterType
import models.behaviors.datatypeconfig.DataTypeConfig
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

  def allFor(config: DataTypeConfig): Future[Seq[DataTypeField]] = {
    val action = allForConfigQuery(config.id).result.map { r =>
      r.map(tuple2Field)
    }
    dataService.run(action)
  }

  def createFor(name: String, fieldType: BehaviorParameterType, config: DataTypeConfig): Future[DataTypeField] = {
    val newInstance = DataTypeField(IDs.next, name, fieldType, config.id)
    val action = (all += newInstance.toRaw).map { _ => newInstance }
    dataService.run(action)
  }

}
