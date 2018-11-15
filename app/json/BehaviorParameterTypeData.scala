package json

import export.BehaviorGroupExporter
import models.behaviors.behaviorparameter.BehaviorParameterType
import models.behaviors.datatypefield.FieldTypeForSchema
import models.behaviors.defaultstorageitem.GraphQLHelpers
import services.DataService

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

  lazy val outputName: String = {
    maybeBuiltinType.map { builtin =>
      builtin.outputName
    }.getOrElse {
      GraphQLHelpers.formatTypeName(name)
    }
  }

  override lazy val inputName: String = {
    maybeBuiltinType.map { builtin =>
      builtin.inputName
    }.getOrElse {
      outputName ++ "Input"
    }
  }

  def copyForExport(groupExporter: BehaviorGroupExporter): BehaviorParameterTypeData = {
    copy(id = None, needsConfig = None, typescriptType = None)
  }

}

object BehaviorParameterTypeData {

  def from(paramType: BehaviorParameterType, dataService: DataService)(implicit ec: ExecutionContext): Future[BehaviorParameterTypeData] = {
    paramType.needsConfig(dataService).map { needsConfig =>
      BehaviorParameterTypeData(Some(paramType.id), Some(paramType.exportId), paramType.name, Some(needsConfig), Some(paramType.typescriptType))
    }
  }

}
