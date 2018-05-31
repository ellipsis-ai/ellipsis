package services

import akka.actor.ActorSystem
import javax.inject.{Inject, Singleton}
import json.Formatting._
import models.accounts.slack.botprofile.SlackBotProfile
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json.{JsError, JsSuccess}
import utils.SlackConversation

import scala.concurrent.{ExecutionContext, Future}

case class MalformedResponseException(message: String) extends Exception(message)
case class SlackApiError(code: String) extends Exception(code)

@Singleton
class SlackApiService @Inject()(services: DefaultServices, implicit val actorSystem: ActorSystem, implicit val ec: ExecutionContext) {

  private val API_BASE_URL = "https://slack.com/api/"
  private val ws = services.ws

  private def urlFor(method: String): String = s"$API_BASE_URL/$method"

  def conversationInfo(profile: SlackBotProfile, convoId: String): Future[Option[SlackConversation]] = {
    ws.
      url(urlFor("conversations.info")).
      withHttpHeaders(HeaderNames.ACCEPT -> MimeTypes.JSON).
      post(Map(
        "token" -> Seq(profile.token),
        "channel" -> Seq(convoId)
      )).
      map { response =>
        (response.json \ "channel").asOpt[SlackConversation]
      }
  }

  def listConversations(profile: SlackBotProfile): Future[Seq[SlackConversation]] = {
    ws.
      url(urlFor("conversations.list")).
      withQueryStringParameters(("token", profile.token), ("limit", "1000"), ("types", "public_channel, private_channel, mpim, im"), ("exclude_archived", "true")).
      get.
      map { response =>
        (response.json \ "channels").validate[Seq[SlackConversation]] match {
          case JsSuccess(data, _) => data
          case JsError(err) => Seq()
        }
      }
  }

  def conversationMembers(profile: SlackBotProfile, convoId: String): Future[Seq[String]] = {
    ws.
      url(urlFor("conversations.members")).
      withHttpHeaders(HeaderNames.ACCEPT -> MimeTypes.JSON).
      post(Map(
        "token" -> Seq(profile.token),
        "channel" -> Seq(convoId)
      )).
      map { response =>
        (response.json \ "members").asOpt[Seq[String]].getOrElse(Seq())
      }
  }

  def openConversationFor(profile: SlackBotProfile, slackUserId: String): Future[String] = {
    ws.
      url(urlFor("conversations.open")).
      withHttpHeaders(HeaderNames.ACCEPT -> MimeTypes.JSON).
      post(Map(
        "token" -> Seq(profile.token),
        "users" -> Seq(slackUserId)
      )).
      map { response =>
        val json = response.json
        if ((json \ "ok").as[Boolean]) {
          (json \ "channel" \ "id").validate[String] match {
            case JsSuccess(id, _) => id
            case JsError(err) => throw MalformedResponseException(err.toString)
          }
        } else {
          throw SlackApiError((json \ "error").as[String])
        }
      }
  }


}
