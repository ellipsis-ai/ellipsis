package models.behaviors.defaultstorageitem

import java.time.OffsetDateTime

import models.behaviors.behavior.Behavior
import play.api.libs.json.JsValue

case class DefaultStorageItem(
                             id: String,
                             behavior: Behavior,
                             updatedAt: OffsetDateTime,
                             updatedByUserId: String,
                             data: JsValue
                             ) {

  def toRaw: RawDefaultStorageItem = {
    RawDefaultStorageItem(
      id,
      behavior.id,
      updatedAt,
      updatedByUserId,
      data
    )
  }
}
