package models.accounts.oauth2application

import models.accounts.oauth2api.OAuth2Api
import org.apache.commons.lang.WordUtils
import play.api.libs.ws.{WSClient, WSRequest}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class OAuth2Application(
                              id: String,
                              name: String,
                              api: OAuth2Api,
                              clientId: String,
                              clientSecret: String,
                              maybeScope: Option[String],
                              teamId: String
                            ) {

  val maybeAuthorizationUrl = api.maybeAuthorizationUrl
  val accessTokenUrl = api.accessTokenUrl
  val scopeString = maybeScope.getOrElse("")

  def maybeAuthorizationRequestFor(state: String, redirectUrl: String, ws: WSClient): Option[WSRequest] = {
    maybeAuthorizationUrl.map { authorizationUrl =>
      ws.url(authorizationUrl).withQueryString(
        "client_id" -> clientId,
        "redirect_uri" -> redirectUrl,
        "scope" -> scopeString,
        "state" -> state,
        "access_type" -> "offline",
        "response_type" -> "code"
      )
    }
  }

  private def clientCredentialsTokenRequestFor(ws: WSClient): WSRequest = {
    ws.url(accessTokenUrl).withQueryString(
      "client_id" -> clientId,
      "client_secret" -> clientSecret,
      "grant_type" -> "client_credentials",
      "scope" -> scopeString
    ).withMethod("POST")
  }

  def getClientCredentialsTokenFor(ws: WSClient): Future[Option[String]] = {
    clientCredentialsTokenRequestFor(ws).execute().map { response =>
      val json = response.json
      (json \ "access_token").asOpt[String]
    }
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
