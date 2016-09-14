package models.bots.invocationlogentry

import org.joda.time.DateTime

case class InvocationLogEntry(
                               id: String,
                               behaviorVersionId: String,
                               resultType: String,
                               resultText: String,
                               context: String,
                               maybeUserIdForContext: Option[String],
                               runtimeInMilliseconds: Long,
                               createdAt: DateTime
                             )
