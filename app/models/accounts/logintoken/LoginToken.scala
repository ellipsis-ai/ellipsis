package models.accounts.logintoken

import org.joda.time.LocalDateTime

case class LoginToken(
                        value: String,
                        userId: String,
                        isUsed: Boolean,
                        createdAt: LocalDateTime
                      ) {

  def isExpired: Boolean = createdAt.isBefore(LoginToken.expiryCutoff)

  def isValid: Boolean = !isUsed && !isExpired

}

object LoginToken {

  val EXPIRY_SECONDS = 300

  def expiryCutoff: LocalDateTime = LocalDateTime.now.minusSeconds(EXPIRY_SECONDS)

}
