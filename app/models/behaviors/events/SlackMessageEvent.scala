package models.behaviors.events

import akka.actor.ActorSystem
import models.accounts.slack.botprofile.SlackBotProfile
import models.accounts.slack.profile.SlackProfile
import models.accounts.user.User
import models.behaviors.conversations.conversation.Conversation
import play.api.libs.json.{JsArray, JsBoolean, JsObject, JsString}
import play.api.libs.ws.WSClient
import services.DataService
import slack.api.{ApiError, SlackApiClient}
import slack.models.Channel
import utils.SlackMessageSender

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.matching.Regex

case class SlackMessageEvent(
                                 profile: SlackBotProfile,
                                 channel: String,
                                 maybeThreadId: Option[String],
                                 user: String,
                                 text: String,
                                 ts: String
                               ) extends MessageEvent with SlackEvent {

  lazy val isBotMessage: Boolean = profile.userId == user

  val messageText: String = text

  override val relevantMessageText: String = {
    val withoutDotDotDot = MessageEvent.ellipsisRegex.replaceFirstIn(messageText, "")
    SlackMessageEvent.toBotRegexFor(profile.userId).replaceFirstIn(withoutDotDotDot, "")
  }

  lazy val includesBotMention: Boolean = {
    isDirectMessage(channel) ||
      SlackMessageEvent.mentionRegexFor(profile.userId).findFirstMatchIn(text).nonEmpty ||
      MessageEvent.ellipsisRegex.findFirstMatchIn(text).nonEmpty
  }

  override val isResponseExpected: Boolean = includesBotMention
  val teamId: String = profile.teamId
  val userIdForContext: String = user


  lazy val maybeChannel = Some(channel)
  lazy val name: String = Conversation.SLACK_CONTEXT

  def isDirectMessage(channelId: String): Boolean = {
    channelId.startsWith("D")
  }
  def isPrivateChannel(channelId: String): Boolean = {
    channel.startsWith("G")
  }

  def maybeOngoingConversation(dataService: DataService): Future[Option[Conversation]] = {
    dataService.conversations.findOngoingFor(user, context, maybeChannel, maybeThreadId, maybeChannel.exists(isDirectMessage))
  }

  def maybeConversationRootedHere(dataService: DataService): Future[Option[Conversation]] = {
    dataService.conversations.findOngoingFor(user, context, maybeChannel, Some(ts), maybeChannel.exists(isDirectMessage))
  }

  override def recentMessages(dataService: DataService)(implicit actorSystem: ActorSystem): Future[Seq[String]] = {
    for {
      maybeTeam <- dataService.teams.find(profile.teamId)
      maybeOAuthToken <- dataService.oauth2Tokens.maybeFullForSlackTeamId(profile.slackTeamId)
      maybeUserClient <- Future.successful(maybeOAuthToken.map { token =>
        SlackApiClient(token.accessToken)
      })
      maybeHistory <- maybeUserClient.map { userClient =>
        userClient.getChannelHistory(channel, latest = Some(ts)).map(Some(_))
      }.getOrElse(Future.successful(None))
      messages <- Future.successful(maybeHistory.map { history =>
        history.messages.slice(0, 10).reverse.flatMap { json =>
          (json \ "text").asOpt[String]
        }
      }.getOrElse(Seq()))
    } yield messages
  }

  def channelForSend(forcePrivate: Boolean, maybeConversation: Option[Conversation], dataService: DataService)(implicit actorSystem: ActorSystem): Future[String] = {
    eventualMaybeDMChannel(dataService)(actorSystem).map { maybeDMChannel =>
      (if (forcePrivate) {
        maybeDMChannel
      } else {
        None
      }).orElse {
        maybeConversation.flatMap { convo =>
          convo.maybeChannel
        }
      }.getOrElse(channel)
    }
  }

  def sendMessage(
                   unformattedText: String,
                   forcePrivate: Boolean,
                   maybeShouldUnfurl: Option[Boolean],
                   maybeConversation: Option[Conversation],
                   maybeActions: Option[MessageActions] = None,
                   dataService: DataService
                 )(implicit actorSystem: ActorSystem): Future[Option[String]] = {
    channelForSend(forcePrivate, maybeConversation, dataService).flatMap { channelToUse =>
      SlackMessageSender(
        clientFor(dataService),
        user,
        unformattedText,
        forcePrivate,
        channel,
        channelToUse,
        maybeThreadId,
        maybeShouldUnfurl,
        maybeConversation,
        maybeActions
      ).send
    }
  }

  override def ensureUser(dataService: DataService): Future[User] = {
    super.ensureUser(dataService).flatMap { user =>
      dataService.slackProfiles.save(SlackProfile(profile.slackTeamId, loginInfo)).map(_ => user)
    }
  }

  private def maybeChannelInfoFor(client: SlackApiClient)(implicit actorSystem: ActorSystem): Future[Option[Channel]] = {
    client.getChannelInfo(channel).map(Some(_)).recover {
      case e: ApiError => None
    }
  }

  override def detailsFor(ws: WSClient, dataService: DataService)(implicit actorSystem: ActorSystem): Future[JsObject] = {
    val client = clientFor(dataService)
    for {
      user <- client.getUserInfo(userIdForContext)
      maybeChannel <- maybeChannelInfoFor(client)
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

  override def unformatTextFragment(text: String): String = {
    // Replace formatted links with their visible text
    text.replaceAll("""<.+?\|(.+?)>""", "$1")
  }

}

object SlackMessageEvent {

  def mentionRegexFor(botId: String): Regex = s"""<@$botId>""".r
  def toBotRegexFor(botId: String): Regex = s"""^<@$botId>:?\\s*""".r

}
