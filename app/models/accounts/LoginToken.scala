package models.accounts

import org.joda.time.DateTime

case class LoginToken(
                        value: String,
                        userId: String,
                        isUsed: Boolean,
                        createdAt: DateTime
                      ) {

  def isExpired: Boolean = createdAt.isBefore(LoginToken.expiryCutoff)

  def isValid: Boolean = !isUsed && !isExpired

}

object LoginToken {

  val EXPIRY_SECONDS = 300

  def expiryCutoff: DateTime = DateTime.now.minusSeconds(EXPIRY_SECONDS)

}
