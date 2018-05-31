package models.behaviors.events

import akka.actor.ActorSystem
import json.Formatting._
import json.SlackUserData
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.conversations.conversation.Conversation
import play.api.Logger
import play.api.libs.json._
import services.{DefaultServices, SlackApiError}
import slack.api.SlackApiClient
import slick.dbio.DBIO
import utils.SlackChannels

import scala.concurrent.{ExecutionContext, Future}

trait SlackEvent {
  val user: String
  val userSlackTeamId: String
  val channel: String
  val profile: SlackBotProfile
  val client: SlackApiClient
  val isUninterruptedConversation: Boolean
  def eventualMaybeDMChannel(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]] = {
    services.slackApiService.openConversationFor(profile, user).map(Some(_)).recover {
      case e: SlackApiError => {
        val msg =
          s"""Couldn't open DM channel to user with ID ${user} on Slack team ${userSlackTeamId} due to Slack API error: ${e.code}
             |Original event channel: $channel
           """.stripMargin
        Logger.error(msg, e)
        None
      }
    }
  }

  def channelForSend(
                      forcePrivate: Boolean,
                      maybeConversation: Option[Conversation],
                      services: DefaultServices
                    )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[String] = {
    (if (forcePrivate) {
      eventualMaybeDMChannel(services)
    } else {
      Future.successful(maybeConversation.flatMap(_.maybeChannel))
    }).map { maybeChannel =>
      maybeChannel.getOrElse(channel)
    }
  }

  def maybeChannelForSendAction(
                                  forcePrivate: Boolean,
                                  maybeConversation: Option[Conversation],
                                  services: DefaultServices
                                )(implicit ec: ExecutionContext, actorSystem: ActorSystem): DBIO[Option[String]] = {
    DBIO.from(channelForSend(forcePrivate, maybeConversation, services).map(Some(_)))
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
    if (isDirectMessage || isUninterruptedConversation) {
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
    val slackChannels = SlackChannels(profile, services.slackApiService)
    for {
      maybeUser <- services.slackEventService.maybeSlackUserDataFor(user, profile.slackTeamId, SlackApiClient(profile.token), (e) => {
        Logger.error(
          s"""Slack API reported user not found while generating details about the user to send to an action:
             |Slack user ID: ${user}
             |Ellipsis bot Slack team ID: ${profile.slackTeamId}
             |Ellipsis team ID: ${profile.teamId}
           """.stripMargin, e)
        None
      })
      maybeChannelInfo <- slackChannels.getInfoFor(channel)
    } yield {
      val channelDetails = JsObject(Seq(
        "channelMembers" -> Json.toJson(maybeChannelInfo.map(_.membersList).getOrElse(Seq())),
        "channelName" -> Json.toJson(maybeChannelInfo.map(_.name))
      ))
      maybeUser.map { user =>
        profileDataFor(user) ++ channelDetails
      }.getOrElse {
        channelDetails
      }
    }
  }

}
