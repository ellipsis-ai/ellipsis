package models.accounts.oauth2application

import models.accounts.oauth2api.OAuth2Api
import org.apache.commons.lang.WordUtils
import play.api.libs.ws.{WSClient, WSRequest}

case class OAuth2Application(
                              id: String,
                              name: String,
                              api: OAuth2Api,
                              clientId: String,
                              clientSecret: String,
                              maybeScope: Option[String],
                              teamId: String
                            ) {

  val authorizationUrl = api.authorizationUrl
  val accessTokenUrl = api.accessTokenUrl
  val scopeString = maybeScope.getOrElse("")

  def authorizationRequestFor(state: String, redirectUrl: String, ws: WSClient): WSRequest = {
    ws.url(authorizationUrl).withQueryString(
      "client_id" -> clientId,
      "redirect_uri" -> redirectUrl,
      "scope" -> scopeString,
      "state" -> state,
      "access_type" -> "offline",
      "response_type" -> "code"
    )
  }

  def accessTokenRequestFor(code: String, redirectUrl: String, ws: WSClient): WSRequest = {
    ws.url(accessTokenUrl).withQueryString(
      "client_id" -> clientId,
      "client_secret" -> clientSecret,
      "code" -> code,
      "grant_type" -> "authorization_code",
      "redirect_uri" -> redirectUrl)
  }

  def refreshTokenRequestFor(refreshToken: String, ws: WSClient): WSRequest = {
    ws.url(accessTokenUrl).withQueryString(
      "refresh_token" -> refreshToken,
      "client_id" -> clientId,
      "client_secret" -> clientSecret,
      "grant_type" -> "refresh_token"
    )
  }

  def keyName: String = {
    val capitalized = WordUtils.capitalize(name).replaceAll("\\s", "")
    capitalized.substring(0, 1).toLowerCase() + capitalized.substring(1)
  }

  def toRaw = RawOAuth2Application(
    id,
    name,
    api.id,
    clientId,
    clientSecret,
    maybeScope,
    teamId
  )
}
