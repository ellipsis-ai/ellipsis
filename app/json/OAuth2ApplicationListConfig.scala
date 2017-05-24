package json

case class OAuth2ApplicationListConfig(
  containerId: String,
  csrfToken: Option[String],
  teamId: String,
  apis: Seq[OAuth2ApiData],
  applications: Seq[OAuth2ApplicationData]
)
