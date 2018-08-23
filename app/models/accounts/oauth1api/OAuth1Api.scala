package models.accounts.oauth1api

case class OAuth1Api(
                      id: String,
                      name: String,
                      requestTokenUrl: String,
                      accessTokenUrl: String,
                      authorizationUrl: String,
                      maybeNewApplicationUrl: Option[String],
                      maybeTeamId: Option[String]
                    ) {

}
