package json

import java.time.OffsetDateTime

import models.apitoken.APIToken

case class APITokenData(
                         id: String,
                         label: String,
                         maybeExpirySeconds: Option[Int],
                         isOneTime: Boolean,
                         lastUsed: Option[OffsetDateTime],
                         createdAt: OffsetDateTime,
                         isRevoked: Boolean
                       )

object APITokenData {
  def from(token: APIToken): APITokenData = {
    APITokenData(token.id, token.label, token.maybeExpirySeconds, token.isOneTime, token.maybeLastUsed, token.createdAt, token.isRevoked)
  }
}
