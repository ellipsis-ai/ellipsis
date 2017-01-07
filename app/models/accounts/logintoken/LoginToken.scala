package models.accounts.logintoken

import java.time.ZonedDateTime

case class LoginToken(
                        value: String,
                        userId: String,
                        isUsed: Boolean,
                        createdAt: ZonedDateTime
                      ) {

  def isExpired: Boolean = createdAt.isBefore(LoginToken.expiryCutoff)

  def isValid: Boolean = !isUsed && !isExpired

}

object LoginToken {

  val EXPIRY_SECONDS = 300

  def expiryCutoff: ZonedDateTime = ZonedDateTime.now.minusSeconds(EXPIRY_SECONDS)

}
