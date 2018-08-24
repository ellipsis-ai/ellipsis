package json

case class OAuth2ApplicationEditConfig(
                                       apis: Seq[OAuth2ApiData],
                                       callbackUrl: String,
                                       requiresAuth: Option[Boolean] = None,
                                       applicationClientId: Option[String] = None,
                                       applicationClientSecret: Option[String] = None
                                     )
