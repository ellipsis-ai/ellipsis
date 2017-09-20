package models.behaviors.events

import akka.actor.ActorSystem
import models.accounts.slack.botprofile.SlackBotProfile
import play.api.libs.json._
import play.api.libs.ws.WSClient
import services.{CacheService, DataService}
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

  def detailsFor(ws: WSClient, dataService: DataService, cacheService: CacheService)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[JsObject] = {
    for {
      maybeUser <- dataService.linkedAccounts.maybeSlackUserDataFor(user, profile.slackTeamId, SlackApiClient(profile.token))
      channelMembers <- SlackChannels(client, cacheService, profile.slackTeamId).getMembersFor(channel)
    } yield {
      val channelMembersObj = JsObject(Seq(
        "channelMembers" -> JsArray(channelMembers.map(JsString.apply))
      ))
      maybeUser.map { user =>
        user.profile ++ channelMembersObj
      }.getOrElse {
        channelMembersObj
      }
    }
  }

}
