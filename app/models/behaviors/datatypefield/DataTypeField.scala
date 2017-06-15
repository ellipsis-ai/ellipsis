package models.behaviors.datatypefield

import models.behaviors.behaviorparameter.BehaviorParameterType
import models.behaviors.defaultstorageitem.GraphQLHelpers

case class DataTypeField(
                          id: String,
                          name: String,
                          fieldType: BehaviorParameterType,
                          configId: String
                         ) {

  def graphQLOutputName: String = GraphQLHelpers.formatFieldName(name)

  def graphQLOutput: String = s"$graphQLOutputName: ${fieldType.graphQLOutputName}"
  def graphQLInput: String = s"$graphQLOutputName: ${fieldType.graphQLInputName}"

  def toRaw: RawDataTypeField = {
    RawDataTypeField(id, name, fieldType.id, configId)
  }

}
