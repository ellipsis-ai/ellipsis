package models.accounts.oauth2token

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.OAuth2Info
import org.joda.time.{DateTime, Seconds}

case class OAuth2Token(
                        accessToken: String,
                        maybeSlackScopes: Option[String],
                        maybeTokenType: Option[String],
                        maybeExpirationTime: Option[DateTime],
                        maybeRefreshToken: Option[String],
                        loginInfo: LoginInfo
                      ) {
  def maybeOauth2Params: Option[Map[String, String]] = {
    maybeSlackScopes.map { scopes =>
      Map("scope" -> scopes)
    }
  }
  def oauth2Info: OAuth2Info = OAuth2Info(accessToken, maybeTokenType, expiresIn, maybeRefreshToken, maybeOauth2Params)
  def expiresIn: Option[Int] = maybeExpirationTime.map { expirationTime =>
    val now = DateTime.now
    if (expirationTime.isAfter(now)) {
      Seconds.secondsBetween(now, expirationTime).getSeconds
    } else {
      0
    }
  }
}
