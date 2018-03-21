package json

case class OAuth2ApplicationEditConfig(
  containerId: String,
  csrfToken: Option[String],
  isAdmin: Boolean,
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
  applicationShared: Boolean = false,
  applicationCanBeShared: Boolean = false,
  recommendedScope: Option[String] = None,
  requiredNameInCode: Option[String] = None,
  behaviorGroupId: Option[String] = None,
  behaviorId: Option[String] = None
)
