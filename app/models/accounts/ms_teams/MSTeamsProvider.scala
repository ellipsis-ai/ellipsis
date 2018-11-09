package models.accounts.ms_teams

import com.mohiva.play.silhouette.api.util.HTTPLayer
import com.mohiva.play.silhouette.impl.exceptions.UnexpectedResponseException
import com.mohiva.play.silhouette.impl.providers.OAuth2Provider._
import com.mohiva.play.silhouette.impl.providers._
import models.accounts.ms_teams.profile.{MSTeamsProfile, MSTeamsProfileBuilder, MSTeamsProfileParser}
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json._
import play.api.libs.ws.WSResponse
import services.ms_teams.MSTeamsApiService

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class MSTeamsProvider(
                       protected val httpLayer: HTTPLayer,
                       protected val stateHandler: SocialStateHandler,
                       val settings: OAuth2Settings,
                       apiService: MSTeamsApiService
                     ) extends OAuth2Provider with MSTeamsProfileBuilder {
  import MSTeamsProvider._

  override type Self = MSTeamsProvider
  override type Content = JsValue

  def id = MSTeamsProvider.ID

  override val profileParser = new MSTeamsProfileParser

  override def withSettings(f: (Settings) => Settings) = new MSTeamsProvider(httpLayer, stateHandler, f(settings), apiService)

  protected def urls: Map[String, String] = Map(
    "org" -> ORGANIZATION_API,
    "identity" -> IDENTITY_API
  )

  protected def buildProfile(authInfo: A): Future[MSTeamsProfile] = {
    for {
      identityJson <- urlWithToken(urls("identity"), authInfo).get.map(_.json)
      orgJson <- orgJsonFor(authInfo)
      result <- {
        val combinedJson = identityJson.as[JsObject] ++ Json.obj("org" -> orgJson)
        profileParser.parse(combinedJson, authInfo)
      }
    } yield result
  }

  override protected def buildInfo(response: WSResponse): Try[OAuth2Info] = {
    response.json.validate[OAuth2Info].asEither.fold(
      error => Failure(new UnexpectedResponseException(InvalidInfoFormat.format(id, error))),
      info => Success(info)
    )
  }

  private def urlWithToken(url: String, authInfo: OAuth2Info) = {
    httpLayer.url(url).
      withHttpHeaders(
        HeaderNames.AUTHORIZATION -> s"Bearer ${authInfo.accessToken}",
        HeaderNames.CONTENT_TYPE -> MimeTypes.JSON
      )
  }

  def orgJsonFor(authInfo: OAuth2Info): Future[JsValue] = {
    urlWithToken(urls("org"), authInfo).get().map (_.json)
  }

}


object MSTeamsProvider {

  val ID = "ms_teams"
  val ORGANIZATION_API = "https://graph.microsoft.com/beta/organization"
  val IDENTITY_API = "https://graph.microsoft.com/beta/me"

  val SpecifiedProfileError = "[Silhouette][%s] Error retrieving profile information. Error message: %s"

  def dumpParamsFromAuthInfo(authInfo: OAuth2Info): String = {
    authInfo.params.map(_.mkString("Received these auth params:\n", "\n", "\n")).getOrElse("No auth params present.")
  }
}
