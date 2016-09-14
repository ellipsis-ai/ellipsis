package models.bots.invocationtoken

import org.joda.time.DateTime

case class InvocationToken(
                            id: String,
                            teamId: String,
                            isUsed: Boolean,
                            createdAt: DateTime
                          ) {
  def isExpired: Boolean = createdAt.isBefore(DateTime.now.minusSeconds(30))
}
