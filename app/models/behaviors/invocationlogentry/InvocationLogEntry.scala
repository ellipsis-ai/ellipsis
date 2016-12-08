package models.behaviors.invocationlogentry

import models.behaviors.behaviorversion.BehaviorVersion
import org.joda.time.LocalDateTime

case class InvocationLogEntry(
                               id: String,
                               behaviorVersion: BehaviorVersion,
                               resultType: String,
                               messageText: String,
                               resultText: String,
                               context: String,
                               maybeUserIdForContext: Option[String],
                               runtimeInMilliseconds: Long,
                               createdAt: LocalDateTime
                             )
