package models.behaviors.datatypefield

import models.behaviors.defaultstorageitem.GraphQLHelpers
import services.DataService
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext

trait DataTypeFieldForSchema {

  val name: String
  def fieldTypeForSchema(dataService: DataService)(implicit ec: ExecutionContext): DBIO[FieldTypeForSchema]

  def outputName: String = GraphQLHelpers.formatFieldName(name)

  def output(dataService: DataService)(implicit ec: ExecutionContext): DBIO[String] = {
    for {
      fieldType <- fieldTypeForSchema(dataService)
      typeOutputName <- fieldType.outputName(dataService)
    } yield {
      s"$outputName: $typeOutputName"
    }
  }
  def input(dataService: DataService)(implicit ec: ExecutionContext): DBIO[String] = {
    for {
      fieldType <- fieldTypeForSchema(dataService)
      typeInputName <- fieldType.inputName(dataService)
    } yield {
      s"$outputName: $typeInputName"
    }
  }

}
