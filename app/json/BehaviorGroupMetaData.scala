package json

import java.time.OffsetDateTime

import models.behaviors.events.UserData

case class BehaviorGroupMetaData(groupId: String, initialCreatedAt: OffsetDateTime, initialAuthor: Option[UserData])
