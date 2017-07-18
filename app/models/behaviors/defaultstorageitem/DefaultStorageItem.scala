package models.behaviors.defaultstorageitem

import java.time.OffsetDateTime

import models.behaviors.behavior.Behavior
import play.api.libs.json.{JsObject, JsString, JsValue}

case class DefaultStorageItem(
                             id: String,
                             behavior: Behavior,
                             updatedAt: OffsetDateTime,
                             updatedByUserId: String,
                             data: JsValue
                             ) {

  def dataWithId: JsValue = {
    data match {
      case obj: JsObject => obj + ("id", JsString(id))
      case _ => data
    }
  }

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
