package models

import com.mohiva.play.silhouette.api.util.HTTPLayer
import com.mohiva.play.silhouette.impl.exceptions.UnexpectedResponseException
import com.mohiva.play.silhouette.impl.providers.OAuth2Provider._
import com.mohiva.play.silhouette.impl.providers._
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSResponse

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class SlackProvider(protected val httpLayer: HTTPLayer,
                    protected val stateProvider: OAuth2StateProvider,
                    val settings: OAuth2Settings) extends OAuth2Provider with SlackProfileBuilder {
  import SlackProvider._

  override type Self = SlackProvider
  override type Content = JsValue

  def id = SlackProvider.ID

  override val profileParser = new SlackProfileParser

  override def withSettings(f: (Settings) => Settings) = new SlackProvider(httpLayer, stateProvider, f(settings))

  protected def urls: Map[String, String] = Map("user" -> USER_API, "auth_test" -> AUTH_TEST_API)

  protected def buildProfile(authInfo: A): Future[SlackProfile] = {
    httpLayer.url(urls("auth_test").format(authInfo.accessToken)).get().flatMap { response =>
      val json = response.json
      profileParser.parse(json)
    }
  }

  override protected def buildInfo(response: WSResponse): Try[OAuth2Info] = {
    val json = response.json
    val maybeTeamName = (json \ "team_name").asOpt[String].map(("team_name", _))
    val maybeScope = (json \ "scope").asOpt[String].map(("scope", _))
    val maybeTeamId = (json \ "team_id").asOpt[String].map(("team_id", _))
    val maybeBot = (json \ "bot").asOpt[JsValue].map { jsValue =>
      val string = Json.stringify(jsValue)
      ("bot", string)
    }
    val maybeIncomingWebhook = (json \ "incoming_webhook").asOpt[JsValue].map { jsValue =>
      val string = Json.stringify(jsValue)
      ("incoming_webhook", string)
    }

    val params = Seq(maybeTeamId, maybeScope, maybeTeamName, maybeBot, maybeIncomingWebhook).flatten.toMap
    response.json.validate[OAuth2Info].asEither.fold(
      error => Failure(new UnexpectedResponseException(InvalidInfoFormat.format(id, error))),
      info => Success(info.copy(params=Some(params)))
    )
  }

  def maybeBotProfileFor(authInfo: OAuth2Info): Option[SlackBotProfile] = {
    val maybeBotJson = authInfo.params.flatMap { params =>
      params.
        find { case (k, v) => k == "bot" }.
        map { case(k, v) => Json.parse(v) }
    }
    val maybeTeamId = authInfo.params.flatMap { params =>
      params.
        find { case(k, v) => k == "team_id" }.
        map { case(k, v) => v }
    }
    for {
      botJson <- maybeBotJson
      userId <- (botJson \ "bot_user_id").asOpt[String]
      token <- (botJson \ "bot_access_token").asOpt[String]
      teamId <- maybeTeamId
    } yield SlackBotProfile(userId, teamId, token)
  }

}


object SlackProvider {
  /*
  Example post:

  https://slack.com/api/chat.postMessage?token=TOKEN&channel=%23general&text=Maybe%20because%20that%20was%20previously%20unfurled:%20https%3A%2F%2Fscatterdot.com%2Ftopic%2F9783365fd925712&username=scatterbot&unfurl_media=true&icon_emoji=:red_circle:&unfurl_links=true
   */
  val ID = "slack"
  val USER_API = "https://slack.com/api/users.info?token=%s&user=%s"
  val AUTH_TEST_API = "https://slack.com/api/auth.test?token=%s"

  val SpecifiedProfileError = "[Silhouette][%s] Error retrieving profile information. Error message: %s"
}
