package models.behaviors.invocationlogentry

import models.behaviors.behaviorversion.BehaviorVersion
import org.joda.time.LocalDateTime
import play.api.libs.json.JsValue

case class InvocationLogEntry(
                               id: String,
                               behaviorVersion: BehaviorVersion,
                               resultType: String,
                               messageText: String,
                               maybeParamValues: Option[JsValue],
                               resultText: String,
                               context: String,
                               maybeUserIdForContext: Option[String],
                               runtimeInMilliseconds: Long,
                               createdAt: LocalDateTime
                             )
