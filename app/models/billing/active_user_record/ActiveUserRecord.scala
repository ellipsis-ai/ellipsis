package models.billing.active_user_record

import java.time.OffsetDateTime

import models.IDs
import play.api.libs.json.{JsValue, Json}


case class ActiveUserRecord(id: String, userId: String, createdAt: OffsetDateTime)

object ActiveUserRecord {
  def apply(userId: String): ActiveUserRecord = ActiveUserRecord(IDs.next, userId, OffsetDateTime.now)
}
