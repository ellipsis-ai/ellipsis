package models.accounts.oauth1application

import com.mohiva.play.silhouette.api.actions.SecuredRequest
import controllers.web.settings.routes
import models.accounts.OAuthApplication
import models.accounts.oauth1api.OAuth1Api
import models.silhouette.EllipsisEnv
import play.api.mvc.AnyContent

case class OAuth1Application(
                              id: String,
                              name: String,
                              api: OAuth1Api,
                              consumerKey: String,
                              consumerSecret: String,
                              maybeScope: Option[String],
                              teamId: String,
                              isShared: Boolean,
                              maybeSharedTokenUserId: Option[String]
                            ) extends OAuthApplication {

  val maybeCustomHost: Option[String] = None

  val key: String = consumerKey
  val secret: String = consumerSecret
  def maybeTokenSharingAuthUrl(implicit request: SecuredRequest[EllipsisEnv, AnyContent]): Option[String] = {
    val redirect = routes.IntegrationsController.shareMyOAuth1Token(id, None).absoluteURL(secure = true)
    Some(controllers.routes.APIAccessController.linkCustomOAuth1Service(id, None, Some(redirect)).absoluteURL(secure = true))
  }

  def toRaw = RawOAuth1Application(
    id,
    name,
    api.id,
    consumerKey,
    consumerSecret,
    maybeScope,
    teamId,
    isShared,
    maybeSharedTokenUserId
  )
}
