package models.accounts.oauth1application

import models.accounts.oauth1api.OAuth1Api

case class OAuth1Application(
                              id: String,
                              name: String,
                              api: OAuth1Api,
                              consumerKey: String,
                              consumerSecret: String,
                              teamId: String,
                              isShared: Boolean
                            ) {

  def toRaw = RawOAuth1Application(
    id,
    name,
    api.id,
    consumerKey,
    consumerSecret,
    teamId,
    isShared
  )
}
