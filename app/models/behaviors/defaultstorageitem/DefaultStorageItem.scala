package models.behaviors.defaultstorageitem

import models.behaviors.behaviorgroup.BehaviorGroup
import play.api.libs.json.JsValue

case class DefaultStorageItem(
                             id: String,
                             typeName: String,
                             behaviorGroup: BehaviorGroup,
                             data: JsValue
                             ) {

  def toRaw: RawDefaultStorageItem = {
    RawDefaultStorageItem(
      id,
      typeName,
      behaviorGroup.id,
      data
    )
  }
}
