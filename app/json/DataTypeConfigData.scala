package json

import models.behaviors.datatypeconfig.{DataTypeConfig, DataTypeConfigForSchema}
import models.behaviors.datatypefield.DataTypeFieldForSchema
import models.behaviors.defaultstorageitem.GraphQLHelpers
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class DataTypeConfigData(
                               name: Option[String],
                               fields: Seq[DataTypeFieldData],
                               usesCode: Option[Boolean]
                             ) extends DataTypeConfigForSchema {

  lazy val typeName: String = name.getOrElse(GraphQLHelpers.fallbackTypeName)

  def copyWithParamTypeIdsIn(oldToNewIdMapping: collection.mutable.Map[String, String]): DataTypeConfigData = {
    copy(fields = fields.map(_.copyWithParamTypeIdsIn(oldToNewIdMapping)))
  }

  def copyForClone: DataTypeConfigData = {
    copy(
      fields = fields.map(_.copyForClone)
    )
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
