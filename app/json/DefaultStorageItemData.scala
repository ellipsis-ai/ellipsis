package json

import java.time.OffsetDateTime

import models.behaviors.defaultstorageitem.DefaultStorageItem
import play.api.libs.json.JsValue

case class DefaultStorageItemData(
                                   id: Option[String],
                                   behaviorId: String,
                                   updatedAt: Option[OffsetDateTime],
                                   updatedByUserId: Option[String],
                                   data: JsValue
                                )

object DefaultStorageItemData {

  def fromItem(item: DefaultStorageItem): DefaultStorageItemData = {
    DefaultStorageItemData(
      Some(item.id),
      item.behavior.id,
      Some(item.updatedAt),
      Some(item.updatedByUserId),
      item.data
    )
  }

}
