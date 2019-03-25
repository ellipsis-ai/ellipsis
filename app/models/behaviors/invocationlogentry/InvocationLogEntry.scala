package models.behaviors.invocationlogentry

import java.time.OffsetDateTime

import models.accounts.user.User
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.events.EventType
import play.api.libs.json.JsValue

case class InvocationLogEntry(
                               id: String,
                               behaviorVersion: BehaviorVersion,
                               resultType: String,
                               maybeEventType: Option[EventType],
                               maybeOriginalEventType: Option[EventType],
                               messageText: String,
                               paramValues: JsValue,
                               resultText: String,
                               context: String,
                               maybeChannel: Option[String],
                               maybeUserIdForContext: Option[String],
                               user: User,
                               runtimeInMilliseconds: Long,
                               createdAt: OffsetDateTime
                             )
