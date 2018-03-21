package json

import java.time.OffsetDateTime

case class BehaviorGroupMetaData(groupId: String, initialCreatedAt: OffsetDateTime, initialAuthor: Option[UserData])
