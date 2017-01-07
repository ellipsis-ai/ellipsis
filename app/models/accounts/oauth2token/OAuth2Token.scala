package models.accounts.oauth2token

import java.time.ZonedDateTime

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.OAuth2Info

case class OAuth2Token(
                        accessToken: String,
                        maybeSlackScopes: Option[String],
                        maybeTokenType: Option[String],
                        maybeExpirationTime: Option[ZonedDateTime],
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
    val now = ZonedDateTime.now
    if (expirationTime.isAfter(now)) {
      expirationTime.getSecond - now.getSecond
    } else {
      0
    }
  }
}
