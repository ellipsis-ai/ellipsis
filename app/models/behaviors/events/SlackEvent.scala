package models.behaviors.events

import akka.actor.ActorSystem
import models.accounts.slack.botprofile.SlackBotProfile
import play.api.libs.json.{JsArray, JsBoolean, JsObject, JsString}
import play.api.libs.ws.WSClient
import services.CacheService
import slack.api.SlackApiClient
import utils.SlackChannels

import scala.concurrent.{ExecutionContext, Future}

trait SlackEvent {
  val user: String
  val channel: String
  val profile: SlackBotProfile
  val client: SlackApiClient
  def eventualMaybeDMChannel(cacheService: CacheService)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]] = {
    SlackChannels(client, cacheService, profile.slackTeamId).listIms.map(_.find(_.user == user).map(_.id))
  }

  val isDirectMessage: Boolean = {
    channel.startsWith("D")
  }
  val isPrivateChannel: Boolean = {
    channel.startsWith("G")
  }
  val isPublicChannel: Boolean = {
    !isDirectMessage && !isPrivateChannel
  }
  val messageRecipientPrefix: String = {
    if (isDirectMessage) {
      ""
    } else {
      s"<@$user>: "
    }
  }

  def detailsFor(ws: WSClient, cacheService: CacheService)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[JsObject] = {
    for {
      user <- client.getUserInfo(user)
      channelMembers <- SlackChannels(client, cacheService, profile.slackTeamId).getMembersFor(channel)
    } yield {
      val profileData = user.profile.map { profile =>
        Seq(
          profile.first_name.map(v => "firstName" -> JsString(v)),
          profile.last_name.map(v => "lastName" -> JsString(v)),
          profile.real_name.map(v => "realName" -> JsString(v))
        ).flatten
      }.getOrElse(Seq())
      JsObject(
        Seq(
          "name" -> JsString(user.name),
          "profile" -> JsObject(profileData),
          "isPrimaryOwner" -> JsBoolean(user.is_primary_owner.getOrElse(false)),
          "isOwner" -> JsBoolean(user.is_owner.getOrElse(false)),
          "isRestricted" -> JsBoolean(user.is_restricted.getOrElse(false)),
          "isUltraRestricted" -> JsBoolean(user.is_ultra_restricted.getOrElse(false)),
          "channelMembers" -> JsArray(channelMembers.map(JsString.apply))
        )
      )
    }
  }

}
