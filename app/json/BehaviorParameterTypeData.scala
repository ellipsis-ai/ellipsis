package json

import export.BehaviorGroupExporter
import models.behaviors.behaviorparameter.BehaviorParameterType
import models.behaviors.datatypefield.FieldTypeForSchema
import models.behaviors.defaultstorageitem.GraphQLHelpers
import services.DataService
import slick.dbio.DBIO

import scala.concurrent.{ExecutionContext, Future}

case class BehaviorParameterTypeData(
                                      id: Option[String],
                                      exportId: Option[String],
                                      name: String,
                                      needsConfig: Option[Boolean],
                                      typescriptType: Option[String]
                                    ) extends FieldTypeForSchema {

  def maybeBuiltinType: Option[BehaviorParameterType] = {
    for {
      typeId <- id
      builtin <- BehaviorParameterType.findBuiltIn(typeId)
    } yield builtin
  }

  def outputName(dataService: DataService)(implicit ec: ExecutionContext): DBIO[String] = {
    maybeBuiltinType.map { builtin =>
      builtin.outputName(dataService)
    }.getOrElse {
      DBIO.successful(GraphQLHelpers.formatTypeName(name))
    }
  }

  override def inputName(dataService: DataService)(implicit ec: ExecutionContext): DBIO[String] = {
    maybeBuiltinType.map { builtin =>
      builtin.inputName(dataService)
    }.getOrElse {
      outputName(dataService).map(_ ++ "Input")
    }
  }

  def copyForExport(groupExporter: BehaviorGroupExporter): BehaviorParameterTypeData = {
    copy(id = None, needsConfig = None, typescriptType = None)
  }

}

object BehaviorParameterTypeData {

  def fromAction(paramType: BehaviorParameterType, dataService: DataService)(implicit ec: ExecutionContext): DBIO[BehaviorParameterTypeData] = {
    for {
      needsConfig <- paramType.needsConfigAction(dataService)
      exportId <- paramType.exportId(dataService)
    } yield {
      BehaviorParameterTypeData(Some(paramType.id), Some(exportId), paramType.name, Some(needsConfig), Some(paramType.typescriptType))
    }
  }

  def from(paramType: BehaviorParameterType, dataService: DataService)(implicit ec: ExecutionContext): Future[BehaviorParameterTypeData] = {
    dataService.run(fromAction(paramType, dataService))
  }

}
