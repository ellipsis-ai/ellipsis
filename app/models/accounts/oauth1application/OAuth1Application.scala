package models.accounts.oauth1application

import models.accounts.OAuthApplication
import models.accounts.oauth1api.OAuth1Api

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
