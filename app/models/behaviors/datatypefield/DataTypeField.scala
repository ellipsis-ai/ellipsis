package models.behaviors.datatypefield

import models.behaviors.behaviorparameter.BehaviorParameterType
import models.behaviors.defaultstorageitem.GraphQLHelpers

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
  lazy val fieldTypeForSchema: FieldTypeForSchema = fieldType

  def toRaw: RawDataTypeField = {
    RawDataTypeField(id, fieldId, name, fieldType.id, configId, rank, isLabel)
  }

}
