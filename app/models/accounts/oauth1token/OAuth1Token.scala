package models.accounts.oauth1token

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers
import com.mohiva.play.silhouette.impl.providers.OAuth1Info

case class OAuth1Token(
                        token: String,
                        secret: String,
                        loginInfo: LoginInfo
                      ) {
  def oauth1Info: OAuth1Info = providers.OAuth1Info(token, secret)

}
