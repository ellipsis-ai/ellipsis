package models.accounts.linkedoauth2token

import java.time.OffsetDateTime

import com.mohiva.play.silhouette.impl.providers.OAuth2Info
import models.accounts.oauth2application.OAuth2Application
import play.api.libs.json.{JsValue, Json}

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

  def isExpiredOrExpiresSoon: Boolean = maybeExpirationTime.exists(_.isBefore(OffsetDateTime.now.plusMinutes(1)))

  def copyWithExpirationTimeIfRefreshToken: LinkedOAuth2Token = {
    val maybeEnsuredExpirationTime = maybeExpirationTime.orElse(maybeRefreshToken.map { _ =>
      OffsetDateTime.now.plusHours(1)
    })
    copy(maybeExpirationTime = maybeEnsuredExpirationTime)
  }

  def copyFrom(info: LinkedOAuth2TokenInfo): LinkedOAuth2Token = {
    copy(
      accessToken = info.accessToken,
      maybeScopeGranted = info.maybeScopeGranted.orElse(maybeScopeGranted),
      maybeExpirationTime = info.maybeExpirationTime,
      maybeTokenType = info.maybeTokenType.orElse(maybeTokenType),
      maybeRefreshToken = info.maybeRefreshToken.orElse(maybeRefreshToken)
    )
  }

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

case class LinkedOAuth2TokenInfo(
                                  accessToken: String,
                                  maybeTokenType: Option[String],
                                  maybeExpirationTime: Option[OffsetDateTime],
                                  maybeRefreshToken: Option[String],
                                  maybeScopeGranted: Option[String]
                                )

object LinkedOAuth2TokenInfo {

  def maybeFrom(json: JsValue): Option[LinkedOAuth2TokenInfo] = {
    (json \ "access_token").asOpt[String].map { accessToken =>
      val maybeTokenType = (json \ "token_type").asOpt[String]
      val maybeScopeGranted = (json \ "scope").asOpt[String]
      val expiresInLookup = (json \ "expires_in")
      val maybeExpiresInSeconds = expiresInLookup.asOpt[Int].orElse {
        expiresInLookup.asOpt[String].flatMap { str =>
          try {
            Some(str.toInt)
          } catch {
            case e: NumberFormatException => None
          }
        }
      }
      val maybeExpirationTime = maybeExpiresInSeconds.map { seconds =>
        OffsetDateTime.now.plusSeconds(seconds)
      }
      val maybeRefreshToken = (json \ "refresh_token").asOpt[String]
      LinkedOAuth2TokenInfo(
        accessToken,
        maybeTokenType,
        maybeExpirationTime,
        maybeRefreshToken,
        maybeScopeGranted
      )
    }
  }

}
