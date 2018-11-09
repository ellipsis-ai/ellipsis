package services.ms_teams

import akka.actor.ActorSystem
import com.fasterxml.jackson.core.JsonParseException
import javax.inject.{Inject, Singleton}
import models.accounts.ms_teams.botprofile.MSTeamsBotProfile
import play.api.Logger
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json._
import play.api.libs.ws.WSResponse
import play.mvc.Http
import play.utils.UriEncoding
import services.DefaultServices
import services.ms_teams.apiModels._

import scala.concurrent.{ExecutionContext, Future}

trait InvalidMSTeamsApiResponseException

case class MSTeamsApiErrorResponseException(status: Int, statusText: String) extends Exception(s"MS Teams API returned ${status}: ${statusText}") with InvalidMSTeamsApiResponseException
case class MalformedMSTeamsApiResponseException(message: String) extends Exception(message) with InvalidMSTeamsApiResponseException
case class MSTeamsApiError(code: String) extends Exception(code)

trait MSTeamsApiClient {
  val tenantId: String
  val maybeEllipsisTeamId: Option[String]
  val services: DefaultServices
  implicit val actorSystem: ActorSystem
  implicit val ec: ExecutionContext

  import Formatting._

  private val API_BASE_URL = "https://graph.microsoft.com/beta/"
  private val ws = services.ws
  private val configuration = services.configuration

  private def encode(segment: String): String = UriEncoding.encodePathSegment(segment, "utf-8")

  private val configPath = "silhouette.ms_teams."
  private val clientId = configuration.get[String](s"${configPath}clientID")
  private val clientSecret = configuration.get[String](s"${configPath}clientSecret")

  private def fetchGraphApiToken: Future[String] = {
    val params = preparePostParams(Map(
      "client_id" -> clientId,
      "scope" -> "https://graph.microsoft.com/.default",
      "client_secret" -> clientSecret,
      "grant_type" -> "client_credentials"
    ))
    ws.
      url(s"https://login.microsoftonline.com/$tenantId/oauth2/v2.0/token").
      post(params).map { res =>
      (res.json \ "access_token").as[String]
    }
  }

  private def fetchBotFrameworkToken: Future[String] = {
    val params = preparePostParams(Map(
      "client_id" -> clientId,
      "scope" -> "https://api.botframework.com/.default",
      "client_secret" -> clientSecret,
      "grant_type" -> "client_credentials"
    ))
    ws.
      url(s"https://login.microsoftonline.com/botframework.com/oauth2/v2.0/token").
      post(params).map { res =>
      (res.json \ "access_token").as[String]
    }
  }

  private def urlFor(method: String): String = s"$API_BASE_URL$method"

  private def responseToJson(response: WSResponse, maybeField: Option[String] = None): JsValue = {
    if (response.status < 400) {
      try {
        response.json
      } catch {
        case j: JsonParseException => throw MalformedMSTeamsApiResponseException(
          s"""Slack API returned a non-JSON response${
            maybeField.map(field => s" while retrieving field ${field}").getOrElse(".")
          }
             |Ellipsis team ID: ${maybeEllipsisTeamId.getOrElse("None")}
             |MS Teams tenant ID: ${tenantId}
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

  private def extract[T](response: WSResponse)(implicit fmt: Format[T]): T = {
    val json = responseToJson(response, None)
    (json \ "value").validate[T] match {
      case JsSuccess(v, _) => v
      case JsError(_) => {
        (json \ "error").validate[String] match {
          case JsSuccess(code, _) => throw MSTeamsApiError(code)
          case JsError(errors) => throw MalformedMSTeamsApiResponseException(
            s"""Error converting MS Teams API data
               |Ellipsis team ID: ${maybeEllipsisTeamId.getOrElse("None")}
               |MS Teams tenant ID: ${tenantId}
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

  private def headersFor(token: String) = {
    Seq(
      HeaderNames.CONTENT_TYPE -> MimeTypes.JSON,
      Http.HeaderNames.AUTHORIZATION -> s"Bearer $token"
    )
  }

  private def postResponseFor(endpoint: String, params: Map[String, Any]): Future[WSResponse] = {
    Logger.info(s"MSTeamsApiClient post $endpoint with params $params")
    for {
      token <- fetchGraphApiToken
      result <- ws.
        url(urlFor(endpoint)).
        withHttpHeaders(headersFor(token): _*).
        post(preparePostParams(params))
    } yield result
  }

  def postToResponseUrl(responseUrl: String, value: JsValue): Future[Unit] = {
    Logger.info(s"MSTeamsApiClient posting response to $responseUrl with value:\n\n${Json.prettyPrint(value)}")
    for {
      token <- fetchBotFrameworkToken
      result <- ws.
        url(responseUrl).
        withHttpHeaders(headersFor(token): _*).
        post(value)
    } yield {
      Logger.info(s"Response to reply:\n\n${result}")
      Unit
    }
  }

  private def getResponseFor(endpoint: String, params: Seq[(String, String)]): Future[WSResponse] = {
    Logger.info(s"MSTeamsApiClient query $endpoint with params $params")
    for {
      token <- fetchGraphApiToken
      result <- ws.
        url(urlFor(endpoint)).
        withHttpHeaders(headersFor(token): _*).
        withQueryStringParameters(params: _*).
        get
    } yield result
  }

  def getOrgInfo: Future[Option[MSTeamsOrganization]] = {
    getResponseFor("organization", Seq()).
      map(r => extract[Seq[MSTeamsOrganization]](r).headOption).
      recover {
        case MSTeamsApiError(err) => {
          Logger.error(
            s"""
               |Failed to retrieve org info: $err
               |
               |Tenant ID: ${tenantId}
             """.stripMargin)
          None
        }
      }
  }

}

case class MSTeamsApiTenantClient(
                           tenantId: String,
                           services: DefaultServices,
                           implicit val actorSystem: ActorSystem,
                           implicit val ec: ExecutionContext
                         ) extends MSTeamsApiClient {

  val maybeEllipsisTeamId: Option[String] = None

}

case class MSTeamsApiProfileClient(
                                   profile: MSTeamsBotProfile,
                                   services: DefaultServices,
                                   implicit val actorSystem: ActorSystem,
                                   implicit val ec: ExecutionContext
                                 ) extends MSTeamsApiClient {

  val maybeEllipsisTeamId: Option[String] = Some(profile.teamId)
  val tenantId: String = profile.tenantId

}

@Singleton
class MSTeamsApiService @Inject()(services: DefaultServices, implicit val actorSystem: ActorSystem, implicit val ec: ExecutionContext) {

  def profileClientFor(profile: MSTeamsBotProfile): MSTeamsApiClient = MSTeamsApiProfileClient(profile, services, actorSystem, ec)
  def tenantClientFor(tenantId: String): MSTeamsApiClient = MSTeamsApiTenantClient(tenantId, services, actorSystem, ec)

  //def adminClient: Future[MSTeamsApiClient] = services.dataService.slackBotProfiles.admin.map(clientFor)

}