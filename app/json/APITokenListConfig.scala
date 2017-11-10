package json

case class APITokenListConfig(
  containerId: String,
  csrfToken: Option[String],
  isAdmin: Boolean,
  teamId: String,
  tokens: Seq[APITokenData],
  justCreatedTokenId: Option[String],
  canGenerateTokens: Boolean
)
