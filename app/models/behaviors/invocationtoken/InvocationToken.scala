package models.behaviors.invocationtoken

import org.joda.time.DateTime

case class InvocationToken(
                            id: String,
                            teamId: String,
                            createdAt: DateTime
                          ) {
  def isExpired: Boolean = createdAt.isBefore(DateTime.now.minusSeconds(10))
}
