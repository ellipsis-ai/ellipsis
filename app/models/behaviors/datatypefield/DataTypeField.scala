package models.behaviors.datatypefield

import models.behaviors.behaviorparameter.BehaviorParameterType
import models.behaviors.defaultstorageitem.GraphQLHelpers

case class DataTypeField(
                          id: String,
                          name: String,
                          fieldType: BehaviorParameterType,
                          configId: String
                         ) {

  def graphQLName: String = GraphQLHelpers.formatFieldName(name)

  def graphQL: String = s"${graphQLName}: ${fieldType.graphQLName}"

  def toRaw: RawDataTypeField = {
    RawDataTypeField(id, name, fieldType.id, configId)
  }

}
