package services.slack

import models.SlackMessageFormatter
import models.accounts.slack.botprofile.SlackBotProfile
import models.accounts.slack.profile.SlackProfile
import models.accounts.user.User
import models.behaviors.conversations.conversation.Conversation
import play.api.libs.json.{JsBoolean, JsObject, JsString}
import play.api.libs.ws.WSClient
import services.DataService
import slack.api.SlackApiClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

case class NewSlackMessageEvent(
                                 profile: SlackBotProfile,
                                 channel: String,
                                 user: String,
                                 text: String,
                                 ts: String
                               ) extends NewMessageEvent with SlackEvent {

  lazy val isBotMessage: Boolean = profile.userId == user

  val fullMessageText: String = text

  lazy val includesBotMention: Boolean = {
    isDirectMessage(channel) ||
      NewSlackMessageEvent.mentionRegexFor(profile.userId).findFirstMatchIn(text).nonEmpty ||
      NewMessageEvent.ellipsisRegex.findFirstMatchIn(text).nonEmpty
  }

  val isResponseExpected: Boolean = includesBotMention
  val teamId: String = profile.teamId
  val userIdForContext: String = user

  lazy val maybeChannel = Some(channel)
  lazy val name: String = Conversation.SLACK_CONTEXT
  def isDirectMessage(channelId: String): Boolean = {
    channelId.startsWith("D")
  }

  def maybeOngoingConversation(dataService: DataService): Future[Option[Conversation]] = {
    dataService.conversations.findOngoingFor(user, conversationContext, maybeChannel.exists(isDirectMessage))
  }

  override def recentMessages(dataService: DataService): Future[Seq[String]] = {
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

  def eventualMaybeDMChannel = client.listIms.map(_.find(_.user == user).map(_.id))

  def maybeChannelFromConversationContext(context: String): Option[String] = {
    s"""${Conversation.SLACK_CONTEXT}#(\\S+)""".r.findFirstMatchIn(context).flatMap { m =>
      m.subgroups.headOption
    }
  }

  def channelForSend(forcePrivate: Boolean, maybeConversation: Option[Conversation]): Future[String] = {
    eventualMaybeDMChannel.map { maybeDMChannel =>
      (if (forcePrivate) {
        maybeDMChannel
      } else {
        None
      }).orElse {
        maybeConversation.flatMap { convo =>
          maybeChannelFromConversationContext(convo.context)
        }
      }.getOrElse(channel)
    }
  }

  private def messageSegmentsFor(formattedText: String): Seq[String] = {
    if (formattedText.length < NewSlackMessageEvent.MAX_MESSAGE_LENGTH) {
      Seq(formattedText)
    } else {
      val largestPossibleSegment = formattedText.substring(0, NewSlackMessageEvent.MAX_MESSAGE_LENGTH)
      val lastNewlineIndex = Math.max(largestPossibleSegment.lastIndexOf('\n'), largestPossibleSegment.lastIndexOf('\r'))
      val lastIndex = if (lastNewlineIndex < 0) { NewSlackMessageEvent.MAX_MESSAGE_LENGTH - 1 } else { lastNewlineIndex }
      Seq(formattedText.substring(0, lastIndex)) ++ messageSegmentsFor(formattedText.substring(lastIndex + 1))
    }
  }

  def sendMessage(
                   unformattedText: String,
                   forcePrivate: Boolean,
                   maybeShouldUnfurl: Option[Boolean],
                   maybeConversation: Option[Conversation]
                 )(implicit ec: ExecutionContext): Future[Unit] = {
    val formattedText = SlackMessageFormatter(client).bodyTextFor(unformattedText)
    channelForSend(forcePrivate, maybeConversation).flatMap { channelToUse =>
      Future.sequence(
        messageSegmentsFor(formattedText).map { ea =>
          // The Slack API considers sending an empty message to be an error rather than a no-op
          if (ea.nonEmpty) {
            for {
              _ <- if (isDirectMessage(channelToUse) && channelToUse != channel) {
                client.postChatMessage(
                  channel,
                  s"<@${user}> I've sent you a private message :sleuth_or_spy:",
                  asUser = Some(true)
                )
              } else {
                Future.successful({})
              }
              _ <- if (!isDirectMessage(channelToUse) && channelToUse != channel) {
                client.postChatMessage(
                  channel,
                  s"<@${user}> OK, back to <#${channelToUse}>",
                  asUser = Some(true)
                )
              } else {
                Future.successful({})
              }
              _ <- client.postChatMessage(
                channelToUse,
                ea,
                asUser = Some(true),
                unfurlLinks = maybeShouldUnfurl,
                unfurlMedia = Some(true)
              )
            } yield {}
          } else {
            Future.successful({})
          }
        }
      )
    }.map(_ => {})
  }

  override def ensureUser(dataService: DataService)(implicit ec: ExecutionContext): Future[User] = {
    super.ensureUser(dataService).flatMap { user =>
      dataService.slackProfiles.save(SlackProfile(profile.slackTeamId, loginInfo)).map(_ => user)
    }
  }

  override def detailsFor(ws: WSClient, dataService: DataService): Future[JsObject] = {
    client.getUserInfo(userIdForContext).map { user =>
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
          "isUltraRestricted" -> JsBoolean(user.is_ultra_restricted.getOrElse(false))
        )
      )
    }
  }

  override def unformatTextFragment(text: String): String = {
    // Replace formatted links with their visible text
    text.replaceAll("""<.+?\|(.+?)>""", "$1")
  }

}

object NewSlackMessageEvent {

  def mentionRegexFor(botId: String): Regex = s"""<@$botId>""".r
  def toBotRegexFor(botId: String): Regex = s"""^<@$botId>:?\\s*""".r

  // From Slack docs:
  //
  // "For best results, message bodies should contain no more than a few thousand characters."
  val MAX_MESSAGE_LENGTH = 2000
}
