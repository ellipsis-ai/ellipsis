package models.behaviors.invocationtoken

import java.time.OffsetDateTime

case class InvocationToken(
                            id: String,
                            userId: String,
                            behaviorId: String,
                            maybeScheduledMessageId: Option[String],
                            createdAt: OffsetDateTime
                          )
