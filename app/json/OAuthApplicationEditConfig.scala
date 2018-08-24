package json

case class OAuthApplicationEditConfig(
                                        containerId: String,
                                        csrfToken: Option[String],
                                        isAdmin: Boolean,
                                        teamId: String,
                                        oauth1Config: OAuth1ApplicationEditConfig,
                                        oauth2Config: OAuth2ApplicationEditConfig,
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
                                        behaviorId: Option[String] = None
                                      )
