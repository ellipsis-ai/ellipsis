package models.accounts

trait OAuthApplication {
  val id: String
  val name: String
  val api: OAuthApi
  val key: String
  val secret: String
  val maybeScope: Option[String]
  val teamId: String
  val isShared: Boolean
}
