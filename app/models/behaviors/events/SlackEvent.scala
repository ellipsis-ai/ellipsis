package models.behaviors.events

import akka.actor.ActorSystem
import models.accounts.slack.botprofile.SlackBotProfile
import play.api.libs.json.{JsArray, JsBoolean, JsObject, JsString}
import play.api.libs.ws.WSClient
import services.CacheService
import slack.api.{ApiError, SlackApiClient}
import slack.models.Channel
import utils.SlackChannels

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait SlackEvent {
  val user: String
  val channel: String
  val profile: SlackBotProfile
  val client: SlackApiClient
  def eventualMaybeDMChannel(implicit actorSystem: ActorSystem) = {
    client.listIms.map(_.find(_.user == user).map(_.id))
  }

  def isDirectMessage(channelId: String): Boolean = {
    channelId.startsWith("D")
  }
  def isPrivateChannel(channelId: String): Boolean = {
    channelId.startsWith("G")
  }
  def messageRecipientPrefixFor(channelId: String): String = {
    if (isDirectMessage(channelId)) {
      ""
    } else {
      s"<@$user>: "
    }
  }

  private def maybeChannelInfoFor(client: SlackApiClient, cacheService: CacheService)(implicit actorSystem: ActorSystem): Future[Option[Channel]] = {
    SlackChannels.maybeChannelInfoFor(channel, client, cacheService)
  }

  def detailsFor(ws: WSClient, cacheService: CacheService)(implicit actorSystem: ActorSystem): Future[JsObject] = {
    for {
      user <- client.getUserInfo(user)
      maybeChannel <- maybeChannelInfoFor(client, cacheService)
    } yield {
      val profileData = user.profile.map { profile =>
        Seq(
          profile.first_name.map(v => "firstName" -> JsString(v)),
          profile.last_name.map(v => "lastName" -> JsString(v)),
          profile.real_name.map(v => "realName" -> JsString(v))
        ).flatten
      }.getOrElse(Seq())
      val channelMembers = maybeChannel.flatMap { channel =>
        channel.members.map(_.filterNot(_ == profile.userId))
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
