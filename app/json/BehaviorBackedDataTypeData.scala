package json

import models.behaviors.behaviorparameter.BehaviorBackedDataType

case class BehaviorBackedDataTypeData(id: Option[String], name: Option[String])

object BehaviorBackedDataTypeData {
  def from(dataType: BehaviorBackedDataType): BehaviorBackedDataTypeData = {
    BehaviorBackedDataTypeData(Some(dataType.id), Some(dataType.name))
  }
}
