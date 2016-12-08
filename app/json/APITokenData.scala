package json

import models.apitoken.APIToken
import org.joda.time.LocalDateTime

case class APITokenData(
                         id: String,
                         label: String,
                         lastUsed: Option[LocalDateTime],
                         createdAt: LocalDateTime,
                         isRevoked: Boolean
                       )

object APITokenData {
  def from(token: APIToken): APITokenData = {
    APITokenData(token.id, token.label, token.maybeLastUsed, token.createdAt, token.isRevoked)
  }
}
