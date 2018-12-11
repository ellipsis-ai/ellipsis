package json

import java.time.OffsetDateTime

import models.behaviors.events.EventUserData

case class BehaviorGroupVersionMetaData(behaviorGroupId: String, createdAt: OffsetDateTime, author: Option[EventUserData])
