package models.accounts.oauth2application

import models.accounts.{OAuth2State, OAuthApplication}
import models.accounts.oauth2api.OAuth2Api
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}

import scala.concurrent.{ExecutionContext, Future}

case class OAuth2Application(
                              id: String,
                              name: String,
                              api: OAuth2Api,
                              clientId: String,
                              clientSecret: String,
                              maybeScope: Option[String],
                              teamId: String,
                              isShared: Boolean,
                              maybeSharedTokenUserId: Option[String]
                            ) extends OAuthApplication {

  val key: String = clientId
  val secret: String = clientSecret

  val maybeAuthorizationUrl = api.maybeAuthorizationUrl
  val accessTokenUrl = api.accessTokenUrl
  val scopeString = maybeScope.getOrElse("")

  def maybeAuthorizationRequestFor(state: OAuth2State, redirectUrl: String, ws: WSClient): Option[WSRequest] = {
    val params = Seq(
      "client_id" -> clientId,
      "redirect_uri" -> redirectUrl,
      "scope" -> scopeString,
      "state" -> state.encodedString,
      "access_type" -> "offline",
      "response_type" -> "code",
      "prompt" -> "consent",
      "include_granted_scopes" -> "true"
    ) ++ api.maybeAudience.map(a => "audience" -> a).toSeq
    maybeAuthorizationUrl.map { authorizationUrl =>
      ws.url(authorizationUrl).withQueryStringParameters(params: _*)
    }
  }

  private def clientCredentialsTokenResponseFor(ws: WSClient): Future[WSResponse] = {
    ws.url(accessTokenUrl).
      withHttpHeaders(HeaderNames.ACCEPT -> MimeTypes.JSON).
      post(Map(
        "client_id" -> Seq(clientId),
        "client_secret" -> Seq(clientSecret),
        "grant_type" -> Seq("client_credentials"),
        "scope" -> Seq(scopeString)
      ))
  }

  def getClientCredentialsTokenFor(ws: WSClient)(implicit ec: ExecutionContext): Future[Option[String]] = {
    clientCredentialsTokenResponseFor(ws).map { response =>
      val json = response.json
      (json \ "access_token").asOpt[String]
    }
  }

  def accessTokenResponseFor(code: String, redirectUrl: String, ws: WSClient): Future[WSResponse] = {
    ws.url(accessTokenUrl).
      withHttpHeaders(HeaderNames.ACCEPT -> MimeTypes.JSON).
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
      withHttpHeaders(HeaderNames.ACCEPT -> MimeTypes.JSON).
      post(Map(
        "refresh_token" -> Seq(refreshToken),
        "client_id" -> Seq(clientId),
        "client_secret" -> Seq(clientSecret),
        "grant_type" -> Seq("refresh_token")
      ))
  }

  def toRaw = RawOAuth2Application(
    id,
    name,
    api.id,
    clientId,
    clientSecret,
    maybeScope,
    teamId,
    isShared,
    maybeSharedTokenUserId
  )
}
