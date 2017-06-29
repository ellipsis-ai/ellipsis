package models.behaviors.defaultstorageitem

import models.behaviors.behavior.Behavior
import play.api.libs.json.JsValue

case class DefaultStorageItem(
                             id: String,
                             behavior: Behavior,
                             data: JsValue
                             ) {

  def toRaw: RawDefaultStorageItem = {
    RawDefaultStorageItem(
      id,
      behavior.id,
      data
    )
  }
}
