package json

import export.BehaviorGroupExporter
import models.behaviors.datatypeconfig.DataTypeConfig
import services.DataService
import slick.dbio.DBIO

import scala.concurrent.{ExecutionContext, Future}

case class DataTypeConfigData(
                               fields: Seq[DataTypeFieldData],
                               usesCode: Option[Boolean]
                             ) {

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

}

object DataTypeConfigData {

  def forConfigAction(config: DataTypeConfig, dataService: DataService)(implicit ec: ExecutionContext): DBIO[DataTypeConfigData] = {
    for {
      fields <- dataService.dataTypeFields.allForAction(config)
      withFieldType <- DBIO.sequence(fields.map { ea =>
        BehaviorParameterTypeData.fromAction(ea.fieldType, dataService).map { fieldTypeData =>
          (ea, fieldTypeData)
        }
      })
    } yield {
      val fieldData = withFieldType.map { case(field, fieldTypeData) =>
        DataTypeFieldData(Some(field.id), Some(field.fieldId), None, field.name, Some(fieldTypeData), field.isLabel)
      }
      DataTypeConfigData(fieldData, config.maybeUsesCode)
    }
  }

  def forConfig(config: DataTypeConfig, dataService: DataService)(implicit ec: ExecutionContext): Future[DataTypeConfigData] = {
    dataService.run(forConfigAction(config, dataService))
  }

  def withDefaultSettings: DataTypeConfigData = {
    DataTypeConfigData(Seq.empty[DataTypeFieldData], usesCode = Some(true))
  }
}
