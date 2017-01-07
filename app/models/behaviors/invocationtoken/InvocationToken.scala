package models.behaviors.invocationtoken

import java.time.OffsetDateTime

case class InvocationToken(
                            id: String,
                            userId: String,
                            behaviorId: String,
                            createdAt: OffsetDateTime
                          ) {
  def isExpired: Boolean = createdAt.isBefore(OffsetDateTime.now.minusSeconds(10))
}
