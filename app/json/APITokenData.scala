package json

import java.time.OffsetDateTime

import models.apitoken.APIToken

case class APITokenData(
                         id: String,
                         label: String,
                         lastUsed: Option[OffsetDateTime],
                         createdAt: OffsetDateTime,
                         isRevoked: Boolean
                       )

object APITokenData {
  def from(token: APIToken): APITokenData = {
    APITokenData(token.id, token.label, token.maybeLastUsed, token.createdAt, token.isRevoked)
  }
}
