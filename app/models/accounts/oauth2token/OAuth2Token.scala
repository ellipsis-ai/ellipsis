package models.accounts.oauth2token

import java.time.OffsetDateTime

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.OAuth2Info

case class OAuth2Token(
                        accessToken: String,
                        maybeSlackScopes: Option[String],
                        maybeTokenType: Option[String],
                        maybeExpirationTime: Option[OffsetDateTime],
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
    val now = OffsetDateTime.now
    if (expirationTime.isAfter(now)) {
      (expirationTime.toEpochSecond - now.toEpochSecond).toInt  // OAuth2Info library class uses Int instead of Long
    } else {
      0
    }
  }
}
