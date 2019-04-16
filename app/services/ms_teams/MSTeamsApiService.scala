package services.ms_teams

import akka.actor.ActorSystem
import com.fasterxml.jackson.core.JsonParseException
import javax.inject.{Inject, Singleton}
import models.accounts.ms_teams.botprofile.MSTeamsBotProfile
import play.api.Logger
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json._
import play.api.libs.ws.WSResponse
import play.api.libs.ws.JsonBodyWritables._
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

  def fetchGraphApiToken: Future[String] = {
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

  def fetchBotFrameworkToken: Future[String] = {
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

  def maybeBotProfile: Option[MSTeamsBotProfile]

  private def urlFor(method: String): String = s"$API_BASE_URL$method"

  private def responseToJson(response: WSResponse, maybeField: Option[String] = None): JsValue = {
    if (response.status < 400) {
      try {
        response.json
      } catch {
        case j: JsonParseException => throw MalformedMSTeamsApiResponseException(
          s"""MS Teams API returned a non-JSON response${
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
        s"""Received irregular response from MS Teams API:
           |${response.status}: ${response.statusText}
           |
           |Truncated body:
           |${response.body.slice(0, 500)}
         """.stripMargin)
      throw MSTeamsApiErrorResponseException(response.status, response.statusText)
    }
  }

  private def extract[T](response: WSResponse, fields: Seq[String])(implicit fmt: Format[T]): T = {
    val json = responseToJson(response, None)
    val lookupResult = fields.foldLeft(json)((json, ea) => (json \ ea).get)
    lookupResult.validate[T] match {
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

  private def extractValue[T](response: WSResponse)(implicit fmt: Format[T]): T = {
    extract[T](response, Seq("value"))
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

  def startConversation(serviceUrl: String, value: StartConversationPayload): Future[String] = {
    for {
      token <- fetchBotFrameworkToken
      result <- ws.
        url(s"$serviceUrl/v3/conversations").
        withHttpHeaders(headersFor(token): _*).
        post(Json.toJson(value))
    } yield {
      val json = responseToJson(result, Some("id"))
      (json \ "id").as[String]
    }
  }

  def postToResponseUrl(responseUrl: String, value: JsValue): Future[String] = {
    Logger.info(s"MSTeamsApiClient posting response to $responseUrl with value:\n\n${Json.prettyPrint(value)}")
    for {
      token <- fetchBotFrameworkToken
      result <- ws.
        url(responseUrl).
        withHttpHeaders(headersFor(token): _*).
        post(value)
    } yield {
      Logger.info(s"Response to message post: ${Json.prettyPrint(result.json)}")
      val json = responseToJson(result, Some("id"))
      (json \ "id").as[String]
    }
  }

  def indicateTyping(info: ActivityInfo): Future[Unit] = {
    val value = Json.toJson(ResponseInfo.newForTyping(
      info.recipient,
      info.conversation,
      info.from
    ))
    for {
      token <- fetchBotFrameworkToken
      _ <- ws.
        url(info.responseUrl).
        withHttpHeaders(headersFor(token): _*).
        post(value)
    } yield {}
  }

  private def messageUrlFor(serviceUrl: String, conversationId: String, activityId: String): String = {
    s"$serviceUrl/v3/conversations/$conversationId/activities/$activityId/"
  }

  def updateMessage(serviceUrl: String, conversationId: String, activityId: String, value: JsValue): Future[String] = {
    val url = messageUrlFor(serviceUrl, conversationId, activityId)
    Logger.info(s"MSTeamsApiClient updating message at $url with value:\n\n${Json.prettyPrint(value)}")
    for {
      token <- fetchBotFrameworkToken
      result <- ws.
        url(url).
        withHttpHeaders(headersFor(token): _*).
        put(value)
    } yield {
      val json = responseToJson(result, Some("id"))
      (json \ "id").as[String]
    }
  }

  def deleteMessage(serviceUrl: String, conversationId: String, activityId: String): Future[String] = {
    val url = messageUrlFor(serviceUrl, conversationId, activityId)
    Logger.info(s"MSTeamsApiClient deleting message at $url")
    for {
      token <- fetchBotFrameworkToken
      result <- ws.
        url(url).
        withHttpHeaders(headersFor(token): _*).
        delete()
    } yield {
      val json = responseToJson(result, Some("id"))
      (json \ "id").as[String]
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
      map(r => extractValue[Seq[MSTeamsOrganization]](r).headOption).
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

  def getApplicationInfo: Future[Option[Application]] = {
    val params = Seq(
      "$filter" -> s"appId eq '$clientId'"
    )
    getResponseFor(s"applications", params).
      map(r => extractValue[Seq[Application]](r).headOption).
      recover {
        case MSTeamsApiError(err) => {
          Logger.error(
            s"""
               |Failed to retrieve application info: $err
               |
               |Application ID: $clientId
               |Tenant ID: ${tenantId}
             """.stripMargin)
          None
        }
      }
  }

  def getUserInfo(userId: String): Future[Option[MSAADUser]] = {
    getResponseFor(s"users/$userId", Seq()).
      map(r => Some(extract[MSAADUser](r, Seq()))).
      recover {
        case MSTeamsApiError(err) => {
          Logger.error(
            s"""
               |Failed to retrieve user info: $err
               |
               |User ID: $userId
               |Tenant ID: ${tenantId}
             """.stripMargin)
          None
        }
        case err: Exception => {
          Logger.info(err.getMessage)
          None
        }

      }
  }

  def getAllTeams: Future[Seq[Team]] = {
    val params = Seq(
      "$filter" -> s"resourceProvisioningOptions/Any(x:x eq 'Team')"
    )
    getResponseFor(s"groups", params).
      map(r => extractValue[Seq[Team]](r)).
      recover {
        case MSTeamsApiError(err) => {
          Logger.error(
            s"""
               |Failed to retrieve teams: $err
               |
               |Tenant ID: ${tenantId}
             """.stripMargin)
          Seq()
        }
      }
  }

  def getAllChannelsFor(team: Team): Future[Seq[Channel]] = {
    getResponseFor(s"teams/${team.id}/channels", Seq()).
      map(r => extractValue[Seq[Channel]](r)).
      recover {
        case MSTeamsApiError(err) => {
          Logger.error(
            s"""
               |Failed to retrieve channels: $err
               |
               |Team ID: ${team.id}
               |Tenant ID: ${tenantId}
             """.stripMargin)
          Seq()
        }
      }
  }

  def getChannelMap: Future[Map[String, ChannelWithTeam]] = {
    for {
      teams <- getAllTeams
      channelsByTeam <- Future.sequence(teams.map { team =>
        getAllChannelsFor(team).map { channels =>
          (team, channels)
        }
      }).map(_.toMap)
    } yield {
      channelsByTeam.flatMap { case(team, channels) =>
        channels.map { channel =>
          (channel.id, ChannelWithTeam(channel, team))
        }
      }
    }
  }

  def getChannelInfo(teamId: String, channelId: String): Future[Option[Channel]] = {
    getResponseFor(s"teams/${encode(teamId)}/channels/${encode(channelId)}", Seq()).
      map(r => Some(extractValue[Channel](r))).
      recover {
        case MSTeamsApiError(err) => {
          Logger.error(
            s"""
               |Failed to retrieve channel info: $err
               |
               |Channel ID: $channelId
               |Tenant ID: ${tenantId}
             """.stripMargin)
          None
        }
      }
  }

  def getTeamMembers(teamId: String): Future[Seq[DirectoryObject]] = {
    getResponseFor(s"groups/${teamId}/members", Seq()).
      map(r => extractValue[Seq[DirectoryObject]](r)).
      recover {
        case MSTeamsApiError(err) => {
          Logger.error(
            s"""
               |Failed to retrieve members: $err
               |
               |Team ID: ${teamId}
               |Tenant ID: ${tenantId}
             """.stripMargin)
          Seq()
        }
      }
  }

  def getTeamMemberDetails(teamId: String): Future[Seq[MSAADUser]] = {
    for {
      members <- getTeamMembers(teamId)
      withDetails <- Future.sequence(members.map { ea =>
        getUserInfo(ea.id)
      }).map(_.flatten)
    } yield withDetails
  }

  def getAllUsers: Future[Seq[MSAADUser]] = {
    getResponseFor(s"users", Seq()).
      map(r => extractValue[Seq[MSAADUser]](r)).
      recover {
        case MSTeamsApiError(err) => {
          Logger.error(
            s"""
               |Failed to retrieve users: $err
               |
               |Tenant ID: ${tenantId}
             """.stripMargin)
          Seq()
        }
      }
  }

  val userIdForContext = services.configuration.get[String]("silhouette.ms_teams.clientID")
  val botIdWithPrefix: String = s"28:${userIdForContext}"

  def botDMDeepLink: String = s"https://teams.microsoft.com/l/chat/0/0?users=${botIdWithPrefix}"

}

case class MSTeamsApiTenantClient(
                                   tenantId: String,
                                   services: DefaultServices,
                                   implicit val actorSystem: ActorSystem,
                                   implicit val ec: ExecutionContext
                                 ) extends MSTeamsApiClient {

  val maybeEllipsisTeamId: Option[String] = None

  def maybeBotProfile: Option[MSTeamsBotProfile] = None


}

case class MSTeamsApiProfileClient(
                                   profile: MSTeamsBotProfile,
                                   services: DefaultServices,
                                   implicit val actorSystem: ActorSystem,
                                   implicit val ec: ExecutionContext
                                 ) extends MSTeamsApiClient {

  val maybeEllipsisTeamId: Option[String] = Some(profile.teamId)
  val tenantId: String = profile.tenantId

  def maybeBotProfile: Option[MSTeamsBotProfile] = Some(profile)


}

@Singleton
class MSTeamsApiService @Inject()(services: DefaultServices, implicit val actorSystem: ActorSystem, implicit val ec: ExecutionContext) {

  def profileClientFor(profile: MSTeamsBotProfile): MSTeamsApiClient = MSTeamsApiProfileClient(profile, services, actorSystem, ec)
  def tenantClientFor(tenantId: String): MSTeamsApiClient = MSTeamsApiTenantClient(tenantId, services, actorSystem, ec)

  //def adminClient: Future[MSTeamsApiClient] = services.dataService.slackBotProfiles.admin.map(clientFor)

}
