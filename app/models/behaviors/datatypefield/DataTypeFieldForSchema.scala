package models.behaviors.datatypefield

import models.behaviors.defaultstorageitem.GraphQLHelpers

trait DataTypeFieldForSchema {

  val name: String
  val fieldTypeForSchema: FieldTypeForSchema

  def outputName: String = GraphQLHelpers.formatFieldName(name)

  def output: String = s"$outputName: ${fieldTypeForSchema.outputName}"
  def input: String = s"$outputName: ${fieldTypeForSchema.inputName}"

}
