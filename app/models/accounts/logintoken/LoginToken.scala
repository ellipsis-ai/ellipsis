package models.accounts.logintoken

import java.time.OffsetDateTime

case class LoginToken(
                        value: String,
                        userId: String,
                        createdAt: OffsetDateTime
                      ) {

  def isExpired: Boolean = createdAt.isBefore(LoginToken.expiryCutoff)

  def isValid: Boolean = !isExpired

}

object LoginToken {

  val EXPIRY_SECONDS = 300

  def expiryCutoff: OffsetDateTime = OffsetDateTime.now.minusSeconds(EXPIRY_SECONDS)

}
