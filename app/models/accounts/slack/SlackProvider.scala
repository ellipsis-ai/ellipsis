package models.accounts.slack

import com.mohiva.play.silhouette.api.util.HTTPLayer
import com.mohiva.play.silhouette.impl.exceptions.UnexpectedResponseException
import com.mohiva.play.silhouette.impl.providers.OAuth2Provider._
import com.mohiva.play.silhouette.impl.providers._
import models.accounts.slack.botprofile.SlackBotProfile
import models.accounts.slack.profile.{SlackProfile, SlackProfileBuilder, SlackProfileParser}
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSResponse
import services.DataService

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class SlackProvider(protected val httpLayer: HTTPLayer,
                    protected val stateHandler: SocialStateHandler,
                    val settings: OAuth2Settings) extends OAuth2Provider with SlackProfileBuilder {
  import SlackProvider._

  override type Self = SlackProvider
  override type Content = JsValue

  def id = SlackProvider.ID

  override val profileParser = new SlackProfileParser

  override def withSettings(f: (Settings) => Settings) = new SlackProvider(httpLayer, stateHandler, f(settings))

  protected def urls: Map[String, String] = Map(
    "user" -> USER_API,
    "team" -> TEAM_API,
    "auth_test" -> AUTH_TEST_API,
    "identity" -> IDENTITY_API
  )

  protected def buildProfile(authInfo: A): Future[SlackProfile] = {
    val scopes = authInfo.params.flatMap(_.get("scope").map(_.split(",")))
    if (scopes.size == 1 && scopes.contains("identity.basic")) {
      httpLayer.url(urls("identity").format(authInfo.accessToken)).get().flatMap { response =>
        profileParser.parseForSignIn(response.json)
      }
    } else {
      httpLayer.url(urls("auth_test").format(authInfo.accessToken)).get().flatMap { response =>
        profileParser.parseForInstall(response.json)
      }
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

  def maybeEnterpriseNameFor(authInfo: OAuth2Info): Future[Option[String]] = {
    httpLayer.url(urls("team").format(authInfo.accessToken)).get().map { response =>
      (response.json \ "team" \ "enterprise_name").asOpt[String]
    }
  }

  def maybeBotProfileFor(slackProfile: SlackProfile, authInfo: OAuth2Info, dataService: DataService): Future[Option[SlackBotProfile]] = {
    val maybeBotJson = authInfo.params.flatMap { params =>
      params.
        find { case (k, v) => k == "bot" }.
        map { case(k, v) => Json.parse(v) }
    }
    val maybeSlackTeamName = authInfo.params.flatMap { params =>
      params.
        find { case(k, v) => k == "team_name" }.
        map { case(k, v) => v }
    }
    val maybeFuture = for {
      botJson <- maybeBotJson
      userId <- (botJson \ "bot_user_id").asOpt[String]
      token <- (botJson \ "bot_access_token").asOpt[String]
      slackTeamName <- maybeSlackTeamName
    } yield {
      maybeEnterpriseNameFor(authInfo).flatMap { maybeEnterpriseName =>
        dataService.slackBotProfiles.ensure(userId, slackProfile.maybeEnterpriseId, slackProfile.firstTeamId, maybeEnterpriseName.getOrElse(slackTeamName), token)
      }
    }

    maybeFuture.map { future =>
      future.map(Some(_))
    }.getOrElse(Future.successful(None))
  }

}


object SlackProvider {
  /*
  Example post:

  https://slack.com/api/chat.postMessage?token=TOKEN&channel=%23general&text=Maybe%20because%20that%20was%20previously%20unfurled:%20https%3A%2F%2Fscatterdot.com%2Ftopic%2F9783365fd925712&username=scatterbot&unfurl_media=true&icon_emoji=:red_circle:&unfurl_links=true
   */
  val ID = "slack"
  val USER_API = "https://slack.com/api/users.info?token=%s&user=%s"
  val TEAM_API = "https://slack.com/api/team.info?token=%s"
  val AUTH_TEST_API = "https://slack.com/api/auth.test?token=%s"
  val IDENTITY_API = "https://slack.com/api/users.identity?token=%s"

  val SpecifiedProfileError = "[Silhouette][%s] Error retrieving profile information. Error message: %s"
}
