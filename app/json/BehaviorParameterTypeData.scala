package json

import export.BehaviorGroupExporter
import models.behaviors.behaviorparameter.{BehaviorParameterType, NumberType, TextType, YesNoType}
import models.behaviors.datatypefield.FieldTypeForSchema
import models.behaviors.defaultstorageitem.GraphQLHelpers
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

case class BehaviorParameterTypeData(
                                      id: Option[String],
                                      exportId: Option[String],
                                      name: String,
                                      needsConfig: Option[Boolean]
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
    copy(id = None, needsConfig = None)
  }

}

object BehaviorParameterTypeData {

  def text: BehaviorParameterTypeData = from(TextType, needsConfig = false)
  def number: BehaviorParameterTypeData = from(NumberType, needsConfig = false)
  def yesNo: BehaviorParameterTypeData = from(YesNoType, needsConfig = false)

  def from(paramType: BehaviorParameterType, needsConfig: Boolean): BehaviorParameterTypeData = {
    BehaviorParameterTypeData(Some(paramType.id), Some(paramType.exportId), paramType.name, Some(needsConfig))
  }

  def from(paramType: BehaviorParameterType, dataService: DataService)(implicit ec: ExecutionContext): Future[BehaviorParameterTypeData] = {
    paramType.needsConfig(dataService).map { needsConfig =>
      from(paramType, needsConfig)
    }
  }

}
