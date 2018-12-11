package json

import java.time.OffsetDateTime

import models.behaviors.events.EventUserData

case class BehaviorGroupMetaData(groupId: String, initialCreatedAt: OffsetDateTime, initialAuthor: Option[EventUserData])
