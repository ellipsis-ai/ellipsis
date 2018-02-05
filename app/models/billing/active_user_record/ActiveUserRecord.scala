package models.billing.active_user_record

import java.time.OffsetDateTime

import models.IDs
import play.api.libs.json.{JsValue, Json}


case class ActiveUserRecord(
                             id: String,
                             teamId: String,
                             userId: Option[String],
                             externalUserId: Option[String],
                             derivedUserId: String,
                             createdAt: OffsetDateTime
                           )

object ActiveUserRecord {
  def apply(teamId: String, userId: Option[String], externalUserId: Option[String]): ActiveUserRecord = {
    ActiveUserRecord(
      IDs.next,
      teamId,
      userId,
      externalUserId,
      userId.getOrElse(externalUserId.get),
      OffsetDateTime.now)
  }
}
