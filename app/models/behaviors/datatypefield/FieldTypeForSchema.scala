package models.behaviors.datatypefield

import services.DataService
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext

trait FieldTypeForSchema {

  def outputName(dataService: DataService)(implicit ec: ExecutionContext): DBIO[String]
  def inputName(dataService: DataService)(implicit ec: ExecutionContext): DBIO[String] = outputName(dataService)

}
