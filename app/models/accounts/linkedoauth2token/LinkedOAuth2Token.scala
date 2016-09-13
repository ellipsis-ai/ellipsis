package models.accounts.linkedoauth2token

import com.mohiva.play.silhouette.impl.providers.OAuth2Info
import models.accounts.oauth2application.OAuth2Application
import org.joda.time.{DateTime, Seconds}

case class LinkedOAuth2Token(
                              accessToken: String,
                              maybeTokenType: Option[String],
                              maybeExpirationTime: Option[DateTime],
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
    val now = DateTime.now
    if (expirationTime.isAfter(now)) {
      Seconds.secondsBetween(now, expirationTime).getSeconds
    } else {
      0
    }
  }

  def isExpired: Boolean = maybeExpirationTime.exists(_.isBeforeNow)

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
