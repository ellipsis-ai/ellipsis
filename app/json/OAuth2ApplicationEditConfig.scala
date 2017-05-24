package json

case class OAuth2ApplicationEditConfig(
  containerId: String,
  csrfToken: Option[String],
  teamId: String,
  apis: Seq[OAuth2ApiData],
  callbackUrl: String,
  mainUrl: String,
  applicationId: String,
  applicationName: Option[String] = None,
  requiresAuth: Option[Boolean] = None,
  applicationClientId: Option[String] = None,
  applicationClientSecret: Option[String] = None,
  applicationScope: Option[String] = None,
  applicationApiId: Option[String] = None,
  applicationSaved: Boolean = false,
  recommendedScope: Option[String] = None,
  behaviorId: Option[String] = None
)
