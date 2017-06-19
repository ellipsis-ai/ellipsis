package models.behaviors.datatypefield

import models.behaviors.behaviorparameter.BehaviorParameterType
import models.behaviors.defaultstorageitem.GraphQLHelpers

case class DataTypeField(
                          id: String,
                          fieldId: String,
                          name: String,
                          fieldType: BehaviorParameterType,
                          configId: String,
                          rank: Int
                         ) {

  def outputName: String = GraphQLHelpers.formatFieldName(name)

  def output: String = s"$outputName: ${fieldType.outputName}"
  def input: String = s"$outputName: ${fieldType.inputName}"

  def toRaw: RawDataTypeField = {
    RawDataTypeField(id, fieldId, name, fieldType.id, configId, rank)
  }

}
