package models.behaviors.invocationlogentry

import java.time.OffsetDateTime

import models.accounts.user.User
import models.behaviors.behaviorversion.BehaviorVersion
import play.api.libs.json.JsValue

case class InvocationLogEntry(
                               id: String,
                               behaviorVersion: BehaviorVersion,
                               resultType: String,
                               messageText: String,
                               paramValues: JsValue,
                               resultText: String,
                               context: String,
                               maybeUserIdForContext: Option[String],
                               maybeUser: Option[User],
                               runtimeInMilliseconds: Long,
                               createdAt: OffsetDateTime
                             )
