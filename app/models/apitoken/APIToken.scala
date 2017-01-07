package models.apitoken

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

case class APIToken(
                     id: String,
                     label: String,
                     userId: String,
                     isRevoked: Boolean,
                     maybeLastUsed: Option[OffsetDateTime],
                     createdAt: OffsetDateTime
                   ) {
  val isValid: Boolean = !isRevoked

  val maybeLastUsedString: Option[String] = maybeLastUsed.map(_.format(APIToken.formatter))
}

object APIToken {
  val formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")
}
