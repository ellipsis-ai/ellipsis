package json

import models.behaviors.behaviorparameter.BehaviorParameterType

case class BehaviorParameterTypeData(id: String, name: String)

object BehaviorParameterTypeData {

  def from(paramType: BehaviorParameterType) = BehaviorParameterTypeData(paramType.id, paramType.name)

}
