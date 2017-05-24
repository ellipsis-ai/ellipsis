package json

case class APITokenListConfig(
  containerId: String,
  csrfToken: Option[String],
  teamId: String,
  tokens: Seq[APITokenData],
  justCreatedTokenId: Option[String]
)
