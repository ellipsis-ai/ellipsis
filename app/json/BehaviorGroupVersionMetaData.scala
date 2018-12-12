package json

import java.time.OffsetDateTime

import models.behaviors.events.UserData

case class BehaviorGroupVersionMetaData(behaviorGroupId: String, createdAt: OffsetDateTime, author: Option[UserData])
