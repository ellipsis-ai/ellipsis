package models.accounts.oauth1application

import com.mohiva.play.silhouette.api.actions.SecuredRequest
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
                              isShared: Boolean
                            ) extends OAuthApplication {

  val key: String = consumerKey
  val secret: String = consumerSecret
  def maybeTokenSharingAuthUrl(implicit request: SecuredRequest[EllipsisEnv, AnyContent]): Option[String] = None

  def toRaw = RawOAuth1Application(
    id,
    name,
    api.id,
    consumerKey,
    consumerSecret,
    maybeScope,
    teamId,
    isShared
  )
}
