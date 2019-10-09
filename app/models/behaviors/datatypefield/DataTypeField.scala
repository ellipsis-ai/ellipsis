package models.behaviors.datatypefield

import models.behaviors.behaviorparameter.BehaviorParameterType
import services.DataService
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext

case class DataTypeField(
                          id: String,
                          fieldId: String,
                          name: String,
                          fieldType: BehaviorParameterType,
                          configId: String,
                          rank: Int,
                          isLabel: Boolean
                         ) extends DataTypeFieldForSchema {

  lazy val isId: Boolean = name == BehaviorParameterType.ID_PROPERTY
  def fieldTypeForSchema(dataService: DataService)(implicit ec: ExecutionContext): DBIO[FieldTypeForSchema] = DBIO.successful(fieldType)

  def toRaw: RawDataTypeField = {
    RawDataTypeField(id, fieldId, name, fieldType.id, configId, rank, isLabel)
  }

}
