package json

import java.time.ZonedDateTime

import models.apitoken.APIToken

case class APITokenData(
                         id: String,
                         label: String,
                         lastUsed: Option[ZonedDateTime],
                         createdAt: ZonedDateTime,
                         isRevoked: Boolean
                       )

object APITokenData {
  def from(token: APIToken): APITokenData = {
    APITokenData(token.id, token.label, token.maybeLastUsed, token.createdAt, token.isRevoked)
  }
}
