package models.apitoken

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

case class APIToken(
                     id: String,
                     label: String,
                     userId: String,
                     isRevoked: Boolean,
                     maybeLastUsed: Option[DateTime],
                     createdAt: DateTime
                   ) {
  val isValid: Boolean = !isRevoked

  val maybeLastUsedString: Option[String] = maybeLastUsed.map(_.toString(APIToken.formatter))
}

object APIToken {
  val formatter = DateTimeFormat.forPattern("MMMM d, yyyy")
}
