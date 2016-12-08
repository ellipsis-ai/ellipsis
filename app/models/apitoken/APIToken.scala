package models.apitoken

import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormat

case class APIToken(
                     id: String,
                     label: String,
                     userId: String,
                     isRevoked: Boolean,
                     maybeLastUsed: Option[LocalDateTime],
                     createdAt: LocalDateTime
                   ) {
  val isValid: Boolean = !isRevoked

  val maybeLastUsedString: Option[String] = maybeLastUsed.map(_.toString(APIToken.formatter))
}

object APIToken {
  val formatter = DateTimeFormat.forPattern("MMMM d, yyyy")
}
