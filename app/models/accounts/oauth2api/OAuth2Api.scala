package models.accounts.oauth2api

case class OAuth2Api(
                      id: String,
                      name: String,
                      authorizationUrl: String,
                      accessTokenUrl: String,
                      maybeNewApplicationUrl: Option[String],
                      maybeScopeDocumentationUrl: Option[String],
                      maybeTeamId: Option[String]
                    ) {

}
