package utils

import akka.actor.ActorSystem
import models.accounts.slack.botprofile.SlackBotProfile
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json.{JsError, JsSuccess}
import play.api.libs.ws.WSClient
import services.caching.CacheService

import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

case class SlackChannels(profile: SlackBotProfile, cacheService: CacheService, ws: WSClient) {

  import json.Formatting._

  def getInfoFor(convoId: String)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[SlackConversation]] = {
    ws.
      url("https://slack.com/api/conversations.info").
      withHttpHeaders(HeaderNames.ACCEPT -> MimeTypes.JSON).
      post(Map(
        "token" -> Seq(profile.token),
        "channel" -> Seq(convoId)
      )).
      map { response =>
        (response.json \ "channel").asOpt[SlackConversation]
      }
  }

  def getList(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Seq[SlackConversation]] = {
    ws.
      url("https://slack.com/api/conversations.list").
      withHttpHeaders(HeaderNames.ACCEPT -> MimeTypes.JSON).
      post(Map(
        "token" -> Seq(profile.token),
        "limit" -> Seq("1000"),
        "type" -> Seq("public_channel, private_channel, mpim, im")
      )).
      map { response =>
        (response.json \ "channels").validate[Seq[SlackConversation]] match {
          case JsSuccess(data, _) => data
          case JsError(err) => {
            println(err)
            Seq()
          }
        }
      }
  }

  def getListForUser(maybeSlackUserId: Option[String])(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Seq[SlackConversation]] = {
    maybeSlackUserId.map { slackUserId =>
      getList.map { channels =>
        channels.filter(ea => ea.visibleToUser(slackUserId))
      }
    }.getOrElse(Future.successful(Seq()))
  }

  def getMembersFor(convoId: String)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Seq[String]] = {
    getInfoFor(convoId).map { maybeConvo =>
      maybeConvo.map(_.membersList).getOrElse(Seq())
    }
  }

  import SlackChannelsRegexes._

  def unformatChannelText(channelText: String): String = {
    channelText match {
      case unformatSlackChannelRegex(channelId) => channelId
      case unformatHashPrefixRegex(channelName) => channelName
      case _ => channelText
    }
  }

  def maybeIdFor(channelLikeIdOrName: String)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]] = {
    val unformattedChannelLikeIdOrName = unformatChannelText(channelLikeIdOrName)
    getInfoFor(unformattedChannelLikeIdOrName).flatMap { maybeChannelLike =>
      maybeChannelLike.map(c => Future.successful(Some(c.id))).getOrElse {
        getList.map { infos =>
          infos.find(_.name == unformattedChannelLikeIdOrName).map(_.id)
        }
      }
    }
  }

}

object SlackChannelsRegexes {

  val unformatSlackChannelRegex: Regex = """<#(.+)\|.+>""".r
  val unformatHashPrefixRegex: Regex = """#(.+)""".r

}
