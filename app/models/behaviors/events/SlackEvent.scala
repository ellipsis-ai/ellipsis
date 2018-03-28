package models.behaviors.events

import akka.actor.ActorSystem
import com.mohiva.play.silhouette.api.LoginInfo
import json.Formatting._
import json.SlackUserData
import models.accounts.slack.botprofile.SlackBotProfile
import models.accounts.slack.profile.SlackProfile
import play.api.libs.json._
import services.caching.CacheService
import services.{DataService, DefaultServices}
import slack.api.SlackApiClient
import utils.SlackChannels

import scala.concurrent.{ExecutionContext, Future}

trait SlackEvent {
  val user: String
  val userSlackTeamId: String
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

  private def profileDataFor(slackUserData: SlackUserData): JsObject = {
    val profile: JsValue = slackUserData.profile.map(Json.toJson(_)).getOrElse(JsObject.empty)
    Json.obj(
      "name" -> slackUserData.getDisplayName,
      "profile" -> profile,
      "isPrimaryOwner" -> slackUserData.isPrimaryOwner,
      "isOwner" -> slackUserData.isOwner,
      "isRestricted" -> slackUserData.isRestricted,
      "isUltraRestricted" -> slackUserData.isUltraRestricted,
      "tz" -> slackUserData.tz
    )
  }

  def detailsFor(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[JsObject] = {
    val slackChannels = SlackChannels(client, services.cacheService, profile.slackTeamId)
    for {
      maybeUser <- services.slackEventService.maybeSlackUserDataFor(user, profile.slackTeamId, SlackApiClient(profile.token))
      maybeChannelInfo <- slackChannels.getInfoFor(channel)
    } yield {
      val channelDetails = JsObject(Seq(
        "channelMembers" -> Json.toJson(maybeChannelInfo.map(_.members).getOrElse(Seq())),
        "channelName" -> Json.toJson(maybeChannelInfo.map(_.name))
      ))
      maybeUser.map { user =>
        profileDataFor(user) ++ channelDetails
      }.getOrElse {
        channelDetails
      }
    }
  }

  def ensureSlackProfileFor(loginInfo: LoginInfo, dataService: DataService)(implicit ec: ExecutionContext): Future[SlackProfile] = {
    dataService.slackProfiles.find(loginInfo).flatMap { maybeExisting =>
      maybeExisting.map(Future.successful).getOrElse {
        dataService.slackProfiles.save(SlackProfile(userSlackTeamId, loginInfo))
      }
    }
  }

}
