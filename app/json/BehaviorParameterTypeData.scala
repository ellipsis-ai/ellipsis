package json

import models.behaviors.behaviorparameter.BehaviorParameterType
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class BehaviorParameterTypeData(
                                      id: String,
                                      name: String,
                                      needsConfig: Option[Boolean]
                                    ) {
  def copyWithIdMapping(mapping: Map[String, String]): BehaviorParameterTypeData = {
    mapping.get(id).map { exportId =>
      copy(id = exportId)
    }.getOrElse(this)
  }
}

object BehaviorParameterTypeData {

  def from(paramType: BehaviorParameterType, dataService: DataService): Future[BehaviorParameterTypeData] = {
    paramType.needsConfig(dataService).map { needsConfig =>
      BehaviorParameterTypeData(paramType.id, paramType.name, Some(needsConfig))
    }
  }

}
