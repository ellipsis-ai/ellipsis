package models.behaviors.invocationlogentry

import java.time.ZonedDateTime

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
                               runtimeInMilliseconds: Long,
                               createdAt: ZonedDateTime
                             )
