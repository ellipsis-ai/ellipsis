package json

import export.BehaviorGroupExporter
import models.behaviors.behaviorparameter.BehaviorParameterType
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class BehaviorParameterTypeData(
                                      id: Option[String],
                                      exportId: Option[String],
                                      name: String,
                                      needsConfig: Option[Boolean]
                                    ) {

  def copyForExport(groupExporter: BehaviorGroupExporter): BehaviorParameterTypeData = {
    copy(id = None, needsConfig = None)
  }

}

object BehaviorParameterTypeData {

  def from(paramType: BehaviorParameterType, dataService: DataService): Future[BehaviorParameterTypeData] = {
    paramType.needsConfig(dataService).map { needsConfig =>
      BehaviorParameterTypeData(Some(paramType.id), Some(paramType.exportId), paramType.name, Some(needsConfig))
    }
  }

}
