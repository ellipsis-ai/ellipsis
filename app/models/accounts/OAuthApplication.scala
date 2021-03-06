package models.accounts

import com.mohiva.play.silhouette.api.actions.SecuredRequest
import models.silhouette.EllipsisEnv
import play.api.mvc.AnyContent

trait OAuthApplication {
  val id: String
  val name: String
  val api: OAuthApi
  val key: String
  val secret: String
  val maybeScope: Option[String]
  val teamId: String
  val isShared: Boolean
  def maybeTokenSharingAuthUrl(implicit request: SecuredRequest[EllipsisEnv, AnyContent]): Option[String]
}
