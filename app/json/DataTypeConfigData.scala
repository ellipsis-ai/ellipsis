package json

import export.BehaviorGroupExporter
import models.behaviors.datatypeconfig.{DataTypeConfig, DataTypeConfigForSchema}
import models.behaviors.datatypefield.DataTypeFieldForSchema
import models.behaviors.defaultstorageitem.GraphQLHelpers
import services.DataService
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class DataTypeConfigData(
                               name: Option[String],
                               fields: Seq[DataTypeFieldData],
                               usesCode: Option[Boolean]
                             ) extends DataTypeConfigForSchema {

  lazy val typeName: String = name.getOrElse(GraphQLHelpers.fallbackTypeName)

  def userDefinedFields: Seq[DataTypeFieldData] = fields.filterNot(_.isBuiltin)

  def copyWithParamTypeIdsIn(oldToNewIdMapping: collection.mutable.Map[String, String]): DataTypeConfigData = {
    copy(fields = userDefinedFields.map(_.copyWithParamTypeIdsIn(oldToNewIdMapping)))
  }

  def copyForClone: DataTypeConfigData = {
    copy(
      fields = userDefinedFields.map(_.copyForClone)
    )
  }

  def copyForNewVersion: DataTypeConfigData = {
    copy(
      fields = userDefinedFields.map(_.copyForNewVersion)
    )
  }

  def copyForExport(groupExporter: BehaviorGroupExporter): DataTypeConfigData = {
    if (usesCode.contains(true)) {
      copy(fields = Seq())
    } else {
      copy(fields = userDefinedFields.map(_.copyForExport(groupExporter)))
    }
  }

  def dataTypeFieldsAction(dataService: DataService): DBIO[Seq[DataTypeFieldForSchema]] = {
    DBIO.successful(fields)
  }

  def dataTypeFields(dataService: DataService): Future[Seq[DataTypeFieldForSchema]] = {
    Future.successful(fields)
  }

}

object DataTypeConfigData {

  def forConfig(config: DataTypeConfig, dataService: DataService): Future[DataTypeConfigData] = {
    for {
      fields <- dataService.dataTypeFields.allFor(config)
      withFieldType <- Future.sequence(fields.map { ea =>
        BehaviorParameterTypeData.from(ea.fieldType, dataService).map { fieldTypeData =>
          (ea, fieldTypeData)
        }
      })
    } yield {
      val fieldData = withFieldType.map { case(field, fieldTypeData) =>
        DataTypeFieldData(Some(field.id), Some(field.fieldId), None, field.name, Some(fieldTypeData), field.isLabel)
      }
      DataTypeConfigData(config.maybeName, fieldData, config.maybeUsesCode)
    }
  }
}
