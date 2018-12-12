package json

import java.time.OffsetDateTime

case class BehaviorGroupVersionMetaData(behaviorGroupId: String, createdAt: OffsetDateTime, author: Option[UserData])
