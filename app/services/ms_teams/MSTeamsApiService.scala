package services.slack

import akka.actor.ActorSystem
import com.fasterxml.jackson.core.JsonParseException
import javax.inject.{Inject, Singleton}
import models.accounts.ms_teams.botprofile.MSTeamsBotProfile
import play.api.Logger
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json._
import play.api.libs.ws.WSResponse
import services.DefaultServices
import services.ms_teams.apiModels._

import scala.concurrent.{ExecutionContext, Future}

trait InvalidMSTeamsApiResponseException

case class MSTeamsApiErrorResponseException(status: Int, statusText: String) extends Exception(s"MS Teams API returned ${status}: ${statusText}") with InvalidMSTeamsApiResponseException
case class MalformedMSTeamsApiResponseException(message: String) extends Exception(message) with InvalidMSTeamsApiResponseException
case class MSTeamsApiError(code: String) extends Exception(code)


case class MSTeamsApiClient(
                           profile: MSTeamsBotProfile,
                           services: DefaultServices,
                           implicit val actorSystem: ActorSystem,
                           implicit val ec: ExecutionContext
                         ) {

  import Formatting._

  val token: String = profile.token

  private val API_BASE_URL = "https://graph.microsoft.com/v1.0/"
  private val ws = services.ws

  private def urlFor(method: String): String = s"$API_BASE_URL/$method"

  private def responseToJson(response: WSResponse, maybeField: Option[String] = None): JsValue = {
    if (response.status < 400) {
      try {
        response.json
      } catch {
        case j: JsonParseException => throw MalformedMSTeamsApiResponseException(
          s"""Slack API returned a non-JSON response${
            maybeField.map(field => s" while retrieving field ${field}").getOrElse(".")
          }
             |Ellipsis team ID: ${profile.teamId}
             |MS Teams org ID: ${profile.teamIdForContext}
             |Error:
             |${j.getMessage}
             |
             |Truncated body:
             |${response.body.slice(0, 500)}
             |""".stripMargin
        )
      }
    } else {
      Logger.error(
        s"""Received irregular response from Slack API:
           |${response.status}: ${response.statusText}
           |
           |Truncated body:
           |${response.body.slice(0, 500)}
         """.stripMargin)
      throw MSTeamsApiErrorResponseException(response.status, response.statusText)
    }
  }

  private def extract[T](response: WSResponse, maybeField: Option[String])(implicit fmt: Format[T]): T = {
    val json = responseToJson(response, maybeField)
    maybeField.map(f => json \ f).getOrElse(json).validate[T] match {
      case JsSuccess(v, _) => v
      case JsError(_) => {
        (json \ "error").validate[String] match {
          case JsSuccess(code, _) => throw MSTeamsApiError(code)
          case JsError(errors) => throw MalformedMSTeamsApiResponseException(
            s"""Error converting MS Teams API data ${maybeField.map(f => s"in field `$f`").getOrElse("")}
               |Ellipsis team ID: ${profile.teamId}
               |MS Teams org ID: ${profile.teamIdForContext}
               |JSON error:
               |${JsError.toJson(errors).toString()}
               """.stripMargin
          )
        }

      }
    }
  }

  private def preparePostParams(params: Map[String,Any]): Map[String,String] = {
    params.flatMap {
      case (k, Some(v)) => Some(k -> v.toString)
      case (k, None) => None
      case (k, v) => Some(k -> v.toString)
    }
  }

  val defaultParams: Seq[(String, String)] = Seq(("token", profile.token))

  private def postResponseFor(endpoint: String, params: Map[String, Any]): Future[WSResponse] = {
    ws.
      url(urlFor(endpoint)).
      withHttpHeaders(HeaderNames.ACCEPT -> MimeTypes.JSON).
      post(preparePostParams(params ++ defaultParams.toMap))
  }

  private def getResponseFor(endpoint: String, params: Seq[(String, String)]): Future[WSResponse] = {
    Logger.info(s"MSTeamsApiClient query $endpoint with params $params")
    ws.
      url(urlFor(endpoint)).
      withHttpHeaders(HeaderNames.ACCEPT -> MimeTypes.JSON).
      withQueryStringParameters((params ++ defaultParams): _*).
      get
  }

  def getOrgInfo: Future[Option[MSTeamsOrganization]] = {
    getResponseFor("organization", Seq()).
      map(r => Some(extract[MSTeamsOrganization](r, None))).
      recover {
        case SlackApiError(err) => {
          Logger.error(
            s"""
               |Failed to retrieve org info: $err
               |
               |Org ID: ${profile.teamIdForContext}
             """.stripMargin)
          None
        }
      }
  }

}

@Singleton
class MSTeamsApiService @Inject()(services: DefaultServices, implicit val actorSystem: ActorSystem, implicit val ec: ExecutionContext) {

  def clientFor(profile: MSTeamsBotProfile): MSTeamsApiClient = MSTeamsApiClient(profile, services, actorSystem, ec)

  //def adminClient: Future[MSTeamsApiClient] = services.dataService.slackBotProfiles.admin.map(clientFor)

}
