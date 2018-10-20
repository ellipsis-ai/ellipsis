package models.behaviors.invocationtoken

import java.time.OffsetDateTime

case class InvocationToken(
                            id: String,
                            userId: String,
                            behaviorVersionId: String,
                            maybeScheduledMessageId: Option[String],
                            maybeTeamIdForContext: Option[String],
                            createdAt: OffsetDateTime
                          )
