package models.behaviors.datatypeconfig

import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.datatypefield.DataTypeFieldForSchema
import models.behaviors.defaultstorageitem.GraphQLHelpers
import services.DataService
import slick.dbio.DBIO

import scala.concurrent.Future

case class DataTypeConfig(
                          id: String,
                          maybeUsesCode: Option[Boolean],
                          behaviorVersion: BehaviorVersion
                        ) extends DataTypeConfigForSchema {

  lazy val maybeName = behaviorVersion.maybeName
  lazy val typeName = maybeName.getOrElse(GraphQLHelpers.fallbackTypeName)

  def usesCode: Boolean = maybeUsesCode.isEmpty || maybeUsesCode.contains(true)

  def dataTypeFieldsAction(dataService: DataService): DBIO[Seq[DataTypeFieldForSchema]] = {
    dataService.dataTypeFields.allForAction(this)
  }

  def dataTypeFields(dataService: DataService): Future[Seq[DataTypeFieldForSchema]] = {
    dataService.run(dataTypeFieldsAction(dataService))
  }

  def toRaw: RawDataTypeConfig = RawDataTypeConfig(id, maybeUsesCode, behaviorVersion.id)
}
