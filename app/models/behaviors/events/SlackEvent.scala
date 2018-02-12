package models.behaviors.events

import akka.actor.ActorSystem
import com.mohiva.play.silhouette.api.LoginInfo
import json.Formatting._
import json.SlackUserData
import models.accounts.slack.botprofile.SlackBotProfile
import models.accounts.slack.profile.SlackProfile
import play.api.libs.json._
import services.{CacheService, DataService, DefaultServices}
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

  private def legacyProfileDataFor(slackUserData: SlackUserData): JsObject = {
    Json.obj(
      "name" -> slackUserData.getDisplayName,
      "profile" -> Json.obj(
        "firstName" -> slackUserData.profile.profile.firstName,
        "lastName" -> slackUserData.profile.profile.lastName,
        "realName" -> slackUserData.profile.profile.realName
      ),
      "isPrimaryOwner" -> slackUserData.profile.isPrimaryOwner,
      "isOwner" -> slackUserData.profile.isOwner,
      "isRestricted" -> slackUserData.profile.isRestricted,
      "isUltraRestricted" -> slackUserData.profile.isUltraRestricted,
      "tz" -> slackUserData.profile.tz
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
        Json.toJson(user.profile).as[JsObject] ++ legacyProfileDataFor(user) ++ channelDetails
      }.getOrElse {
        channelDetails
      }
    }
  }

  def ensureSlackProfileFor(loginInfo: LoginInfo, dataService: DataService)(implicit ec: ExecutionContext): Future[SlackProfile] = {
    dataService.slackProfiles.save(SlackProfile(profile.slackTeamId, loginInfo))
  }

}
