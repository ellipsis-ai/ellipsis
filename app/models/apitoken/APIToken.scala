package models.apitoken

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

case class APIToken(
                     id: String,
                     label: String,
                     userId: String,
                     maybeExpirySeconds: Option[Int],
                     isOneTime: Boolean,
                     isRevoked: Boolean,
                     maybeLastUsed: Option[OffsetDateTime],
                     createdAt: OffsetDateTime
                   ) {

  val isAlreadyUsedOneTime: Boolean = isOneTime && maybeLastUsed.isDefined

  val isExpired: Boolean = maybeExpirySeconds.exists(s => OffsetDateTime.now.minusSeconds(s).isAfter(createdAt))

  val isValid: Boolean = {
    !isRevoked && !isAlreadyUsedOneTime && !isExpired
  }

  val maybeLastUsedString: Option[String] = maybeLastUsed.map(_.format(APIToken.formatter))
}

object APIToken {
  val formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")
}
