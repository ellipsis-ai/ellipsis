package models.accounts

trait OAuthApi {
  val id: String
  val name: String
  val requiresAuth: Boolean
  val accessTokenUrl: String
  val maybeAuthorizationUrl: Option[String]
  val maybeNewApplicationUrl: Option[String]
  val maybeScopeDocumentationUrl: Option[String]
  val maybeTeamId: Option[String]
  val isOAuth1: Boolean
}
