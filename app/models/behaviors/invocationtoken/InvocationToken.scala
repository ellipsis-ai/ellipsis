package models.behaviors.invocationtoken

import org.joda.time.LocalDateTime

case class InvocationToken(
                            id: String,
                            teamId: String,
                            createdAt: LocalDateTime
                          ) {
  def isExpired: Boolean = createdAt.isBefore(LocalDateTime.now.minusSeconds(10))
}
