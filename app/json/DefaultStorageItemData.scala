package json

import java.time.OffsetDateTime

import play.api.libs.json.JsValue

case class DefaultStorageItemData(
                                   id: Option[String],
                                   behaviorId: String,
                                   updatedAt: Option[OffsetDateTime],
                                   updatedByUserId: Option[String],
                                   data: JsValue
                                )
