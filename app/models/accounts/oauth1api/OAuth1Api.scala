package models.accounts.oauth1api

import models.accounts.OAuthApi

case class OAuth1Api(
                      id: String,
                      name: String,
                      requestTokenUrl: String,
                      accessTokenUrl: String,
                      authorizationUrl: String,
                      maybeNewApplicationUrl: Option[String],
                      maybeScopeDocumentationUrl: Option[String],
                      maybeTeamId: Option[String]
                    ) extends OAuthApi {
  val maybeAuthorizationUrl: Option[String] = Some(authorizationUrl)
  val requiresAuth: Boolean = true
  val isOAuth1: Boolean = true
}
