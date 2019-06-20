package json

case class OAuthApplicationEditConfig(
                                        containerId: String,
                                        csrfToken: Option[String],
                                        isAdmin: Boolean,
                                        teamId: String,
                                        apis: Seq[OAuthApiData],
                                        oauth1CallbackUrl: String,
                                        oauth2CallbackUrl: String,
                                        authorizationUrl: String,
                                        requiresAuth: Option[Boolean] = None,
                                        applicationKey: Option[String] = None,
                                        applicationSecret: Option[String] = None,
                                        mainUrl: String,
                                        applicationId: String,
                                        applicationName: Option[String] = None,
                                        applicationApiId: Option[String] = None,
                                        applicationScope: Option[String] = None,
                                        recommendedScope: Option[String] = None,
                                        applicationSaved: Boolean = false,
                                        applicationShared: Boolean = false,
                                        applicationCanBeShared: Boolean = false,
                                        requiredNameInCode: Option[String] = None,
                                        behaviorGroupId: Option[String] = None,
                                        behaviorId: Option[String] = None,
                                        sharedTokenUser: Option[UserData] = None
                                      )
