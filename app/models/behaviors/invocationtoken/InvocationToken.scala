package models.behaviors.invocationtoken

import java.time.ZonedDateTime

case class InvocationToken(
                            id: String,
                            userId: String,
                            behaviorId: String,
                            createdAt: ZonedDateTime
                          ) {
  def isExpired: Boolean = createdAt.isBefore(ZonedDateTime.now.minusSeconds(10))
}
