package json

import models.behaviors.behaviorparameter.BehaviorParameterType
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class BehaviorParameterTypeData(
                                      id: String,
                                      name: String,
                                      needsConfig: Option[Boolean],
                                      behavior: Option[BehaviorVersionData]
                                    ) {

  def copyWithAttachedDataTypeFrom(dataTypes: Seq[BehaviorVersionData]): BehaviorParameterTypeData = {
    copy(behavior = dataTypes.find(_.config.publishedId.contains(id)))
  }

}

object BehaviorParameterTypeData {

  def from(paramType: BehaviorParameterType, dataService: DataService): Future[BehaviorParameterTypeData] = {
    paramType.needsConfig(dataService).map { needsConfig =>
      BehaviorParameterTypeData(paramType.id, paramType.name, Some(needsConfig), None)
    }
  }

}
