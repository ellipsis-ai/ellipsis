package models.accounts.ms_teams

import com.mohiva.play.silhouette.api.util.HTTPLayer
import com.mohiva.play.silhouette.impl.exceptions.UnexpectedResponseException
import com.mohiva.play.silhouette.impl.providers.OAuth2Provider._
import com.mohiva.play.silhouette.impl.providers._
import models.accounts.ms_teams.botprofile.MSTeamsBotProfile
import models.accounts.ms_teams.profile.{MSTeamsProfile, MSTeamsProfileBuilder, MSTeamsProfileParser}
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json._
import play.api.libs.ws.WSResponse
import services.DataService
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

  protected def scopesFromAuth(authInfo: A): Array[String] = {
    authInfo.params.flatMap(_.get("scope").map(_.split(","))).getOrElse(Array.empty)
  }

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
    val jsonTransformer = (__ \ 'expires_in).json.update(
      __.read[String].map{ o => JsNumber(Integer.parseInt(o)) }
    )
    val json = response.json.transform(jsonTransformer).get
    json.validate[OAuth2Info].asEither.fold(
      error => Failure(new UnexpectedResponseException(InvalidInfoFormat.format(id, error))),
      info => Success(info)
    )
  }

  private def urlWithToken(url: String, authInfo: OAuth2Info) = {
    httpLayer.url(url).
      withHttpHeaders(
        HeaderNames.AUTHORIZATION -> s"Bearer ${authInfo.accessToken}",
        HeaderNames.ACCEPT -> MimeTypes.JSON
      )
  }

  def orgJsonFor(authInfo: OAuth2Info): Future[JsValue] = {
    urlWithToken(urls("org"), authInfo).get().map(_.json)
  }

  def maybeOrganizationNameFor(authInfo: OAuth2Info): Future[Option[String]] = {
    orgJsonFor(authInfo).map { json =>
      (json \ "displayName").asOpt[String]
    }
  }

  def maybeBotProfileFor(msTeamsProfile: MSTeamsProfile, authInfo: OAuth2Info, dataService: DataService): Future[Option[MSTeamsBotProfile]] = {
    val maybeBotJson = authInfo.params.flatMap { params =>
      params.
        find { case (k, v) => k == "bot" }.
        map { case(k, v) => Json.parse(v) }
    }
    for {
      maybeOrgName <- maybeOrganizationNameFor(authInfo)
      maybeBotProfile <- (for {
        botJson <- maybeBotJson
        userId <- (botJson \ "bot_user_id").asOpt[String]
        orgName <- maybeOrgName
      } yield {
//        dataService.msTeamsBotProfiles.ensure(
//          userId,
//          msTeamsProfile.teamId,
//          orgName,
//          authInfo.accessToken,
//          OffsetDateTime.now.plusSeconds(authInfo.expiresIn.map(_.toLong).getOrElse(0L)),
//          authInfo.refreshToken.get
//        ).map(Some(_))
        Future.successful(None)
      }).getOrElse(Future.successful(None))
    } yield maybeBotProfile
  }

}


object MSTeamsProvider {
  /*
  Example post:

  https://slack.com/api/chat.postMessage?token=TOKEN&channel=%23general&text=Maybe%20because%20that%20was%20previously%20unfurled:%20https%3A%2F%2Fscatterdot.com%2Ftopic%2F9783365fd925712&username=scatterbot&unfurl_media=true&icon_emoji=:red_circle:&unfurl_links=true
   */
  val ID = "slack"
  val USER_API = "https://slack.com/api/users.info?token=%s&user=%s"
  val ORGANIZATION_API = "https://graph.microsoft.com/v1.0/organization"
  val AUTH_TEST_API = "https://slack.com/api/auth.test?token=%s"
  val IDENTITY_API = "https://graph.microsoft.com/v1.0/me"

  val SpecifiedProfileError = "[Silhouette][%s] Error retrieving profile information. Error message: %s"

  def dumpParamsFromAuthInfo(authInfo: OAuth2Info): String = {
    authInfo.params.map(_.mkString("Received these auth params:\n", "\n", "\n")).getOrElse("No auth params present.")
  }
}
