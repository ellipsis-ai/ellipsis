package models.accounts.linkedoauth1token

import com.mohiva.play.silhouette.impl.providers.OAuth1Info
import models.accounts.oauth1application.OAuth1Application

case class LinkedOAuth1Token(
                              accessToken: String,
                              secret: String,
                              userId: String,
                              application: OAuth1Application
                            ) {

  def oauth1Info: OAuth1Info = OAuth1Info(accessToken, secret)

  def toRaw: RawLinkedOAuth1Token = RawLinkedOAuth1Token(
    accessToken,
    secret,
    userId,
    application.id
  )

}
