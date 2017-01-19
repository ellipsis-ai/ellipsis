package models.behaviors.invocationtoken

import java.time.OffsetDateTime

case class InvocationToken(
                            id: String,
                            userId: String,
                            behaviorId: String,
                            createdAt: OffsetDateTime
                          ) {
  // Ellipsis's function time out is 10 seconds, but there can be a delay in starting, so we allow an extra 5
  def isExpired: Boolean = createdAt.isBefore(OffsetDateTime.now.minusSeconds(15))
}
