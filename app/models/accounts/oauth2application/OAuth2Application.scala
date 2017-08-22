package models.accounts.oauth2application

import models.accounts.oauth2api.OAuth2Api
import org.apache.commons.lang.WordUtils
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import utils.NameFormatter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class OAuth2Application(
                              id: String,
                              name: String,
                              api: OAuth2Api,
                              clientId: String,
                              clientSecret: String,
                              maybeScope: Option[String],
                              teamId: String,
                              isShared: Boolean
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
        "response_type" -> "code",
        "prompt" -> "consent",
        "include_granted_scopes" -> "true"
      )
    }
  }

  private def clientCredentialsTokenResponseFor(ws: WSClient): Future[WSResponse] = {
    ws.url(accessTokenUrl).
      withHeaders(HeaderNames.ACCEPT -> MimeTypes.JSON).
      post(Map(
        "client_id" -> Seq(clientId),
        "client_secret" -> Seq(clientSecret),
        "grant_type" -> Seq("client_credentials"),
        "scope" -> Seq(scopeString)
      ))
  }

  def getClientCredentialsTokenFor(ws: WSClient): Future[Option[String]] = {
    clientCredentialsTokenResponseFor(ws).map { response =>
      val json = response.json
      (json \ "access_token").asOpt[String]
    }
  }

  def accessTokenResponseFor(code: String, redirectUrl: String, ws: WSClient): Future[WSResponse] = {
    ws.url(accessTokenUrl).
      withHeaders(HeaderNames.ACCEPT -> MimeTypes.JSON).
      post(Map(
        "client_id" -> Seq(clientId),
        "client_secret" -> Seq(clientSecret),
        "code" -> Seq(code),
        "grant_type" -> Seq("authorization_code"),
        "redirect_uri" -> Seq(redirectUrl)
      ))
  }

  def refreshTokenResponseFor(refreshToken: String, ws: WSClient): Future[WSResponse] = {
    ws.url(accessTokenUrl).
      withHeaders(HeaderNames.ACCEPT -> MimeTypes.JSON).
      post(Map(
        "refresh_token" -> Seq(refreshToken),
        "client_id" -> Seq(clientId),
        "client_secret" -> Seq(clientSecret),
        "grant_type" -> Seq("refresh_token")
      ))
  }

  def keyName: String = NameFormatter.formatConfigPropertyName(name)

  def toRaw = RawOAuth2Application(
    id,
    name,
    api.id,
    clientId,
    clientSecret,
    maybeScope,
    teamId,
    isShared
  )
}
