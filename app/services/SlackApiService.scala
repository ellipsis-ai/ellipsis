package services

import akka.actor.ActorSystem
import javax.inject.{Inject, Singleton}
import json.Formatting._
import models.accounts.slack.botprofile.SlackBotProfile
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json.{JsError, JsSuccess}
import utils.SlackConversation

import scala.concurrent.{ExecutionContext, Future}

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
      withHttpHeaders(HeaderNames.ACCEPT -> MimeTypes.JSON).
      post(Map(
        "token" -> Seq(profile.token),
        "limit" -> Seq("1000"),
        "type" -> Seq("public_channel, private_channel, mpim, im")
      )).
      map { response =>
        (response.json \ "channels").validate[Seq[SlackConversation]] match {
          case JsSuccess(data, _) => data
          case JsError(_) => Seq()
        }
      }
  }


}
