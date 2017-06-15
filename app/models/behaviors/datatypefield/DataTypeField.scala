package models.behaviors.datatypefield

import models.behaviors.behaviorparameter.BehaviorParameterType
import models.behaviors.defaultstorageitem.GraphQLHelpers

case class DataTypeField(
                          id: String,
                          name: String,
                          fieldType: BehaviorParameterType,
                          configId: String
                         ) {

  def outputName: String = GraphQLHelpers.formatFieldName(name)

  def output: String = s"$outputName: ${fieldType.outputName}"
  def input: String = s"$outputName: ${fieldType.inputName}"

  def toRaw: RawDataTypeField = {
    RawDataTypeField(id, name, fieldType.id, configId)
  }

}
