package models.accounts.oauth2api

import utils.Enum

case class OAuth2Api(
                      id: String,
                      name: String,
                      grantType: OAuth2GrantType,
                      maybeAuthorizationUrl: Option[String],
                      accessTokenUrl: String,
                      maybeNewApplicationUrl: Option[String],
                      maybeScopeDocumentationUrl: Option[String],
                      maybeTeamId: Option[String]
                    ) {

}

object OAuth2GrantType extends Enum[OAuth2GrantType] {
  val values = List(AuthorizationCode, ClientCredentials)
  def definitelyFind(name: String): OAuth2GrantType = find(name).getOrElse(AuthorizationCode)
}

sealed trait OAuth2GrantType extends OAuth2GrantType.Value {
  val requiresAuth: Boolean
}

case object AuthorizationCode extends OAuth2GrantType {
  val requiresAuth = true
}

case object ClientCredentials extends OAuth2GrantType {
  val requiresAuth = false

}
