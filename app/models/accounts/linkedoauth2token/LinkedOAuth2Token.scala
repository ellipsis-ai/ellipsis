package models.accounts.linkedoauth2token

import java.time.OffsetDateTime

import com.mohiva.play.silhouette.impl.providers.OAuth2Info
import models.accounts.oauth2application.OAuth2Application

case class LinkedOAuth2Token(
                              accessToken: String,
                              maybeTokenType: Option[String],
                              maybeExpirationTime: Option[OffsetDateTime],
                              maybeRefreshToken: Option[String],
                              maybeScopeGranted: Option[String],
                              userId: String,
                              application: OAuth2Application
                            ) {

  val maybeScope: Option[String] = application.maybeScope

  def maybeOauth2Params: Option[Map[String, String]] = {
    maybeScope.map { scopes =>
      Map("scope" -> scopes)
    }
  }
  def oauth2Info: OAuth2Info = OAuth2Info(accessToken, maybeTokenType, expiresIn, maybeRefreshToken, maybeOauth2Params)
  def expiresIn: Option[Int] = maybeExpirationTime.map { expirationTime =>
    val now = OffsetDateTime.now
    if (expirationTime.isAfter(now)) {
      (expirationTime.toEpochSecond - now.toEpochSecond).toInt // OAuth2Info library class uses Int instead of Long
    } else {
      0
    }
  }

  def isExpired: Boolean = maybeExpirationTime.exists(_.isBefore(OffsetDateTime.now))

  def toRaw: RawLinkedOAuth2Token = RawLinkedOAuth2Token(
    accessToken,
    maybeTokenType,
    maybeExpirationTime,
    maybeRefreshToken,
    maybeScopeGranted,
    userId,
    application.id
  )

}
