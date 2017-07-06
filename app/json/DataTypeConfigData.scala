package json

import models.behaviors.datatypeconfig.DataTypeConfig
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class DataTypeConfigData(
                               fields: Seq[DataTypeFieldData],
                               usesCode: Option[Boolean]
                             ) {
  def copyWithParamTypeIdsIn(oldToNewIdMapping: collection.mutable.Map[String, String]): DataTypeConfigData = {
    copy(fields = fields.map(_.copyWithParamTypeIdsIn(oldToNewIdMapping)))
  }

  def copyForClone: DataTypeConfigData = {
    copy(
      fields = fields.map(_.copyForClone)
    )
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
      DataTypeConfigData(fieldData, config.maybeUsesCode)
    }
  }
}
