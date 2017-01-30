package models.behaviors.events

import models.SlackMessageFormatter
import models.accounts.slack.botprofile.SlackBotProfile
import models.accounts.slack.profile.SlackProfile
import models.accounts.user.User
import models.behaviors.conversations.conversation.Conversation
import play.api.libs.json.{JsBoolean, JsObject, JsString}
import play.api.libs.ws.WSClient
import services.DataService
import slack.api.SlackApiClient
import slack.models.Attachment

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
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

  val fullMessageText: String = text

  override def relevantMessageText: String = {
    SlackMessageEvent.toBotRegexFor(profile.userId).replaceFirstIn(super.relevantMessageText, "")
  }

  lazy val includesBotMention: Boolean = {
    isDirectMessage(channel) ||
      SlackMessageEvent.mentionRegexFor(profile.userId).findFirstMatchIn(text).nonEmpty ||
      MessageEvent.ellipsisRegex.findFirstMatchIn(text).nonEmpty
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
    dataService.conversations.findOngoingFor(user, context, maybeChannel, maybeThreadId, maybeChannel.exists(isDirectMessage))
  }

  def maybeConversationRootedHere(dataService: DataService): Future[Option[Conversation]] = {
    dataService.conversations.findOngoingFor(user, context, maybeChannel, Some(ts), maybeChannel.exists(isDirectMessage))
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

  def channelForSend(forcePrivate: Boolean, maybeConversation: Option[Conversation]): Future[String] = {
    eventualMaybeDMChannel.map { maybeDMChannel =>
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

  private def messageSegmentsFor(formattedText: String): List[String] = {
    if (formattedText.length < SlackMessageEvent.MAX_MESSAGE_LENGTH) {
      List(formattedText)
    } else {
      val largestPossibleSegment = formattedText.substring(0, SlackMessageEvent.MAX_MESSAGE_LENGTH)
      val lastNewlineIndex = Math.max(largestPossibleSegment.lastIndexOf('\n'), largestPossibleSegment.lastIndexOf('\r'))
      val lastIndex = if (lastNewlineIndex < 0) { SlackMessageEvent.MAX_MESSAGE_LENGTH - 1 } else { lastNewlineIndex }
      (formattedText.substring(0, lastIndex)) :: messageSegmentsFor(formattedText.substring(lastIndex + 1))
    }
  }

  def sendPreamble(formattedText: String, channelToUse: String, maybeConversation: Option[Conversation])(implicit ec: ExecutionContext): Future[Unit] = {
    if (formattedText.nonEmpty) {
      for {
        _ <- maybeConversation.map { convo =>
          if (convo.state == Conversation.DONE_STATE && convo.maybeThreadId.isDefined) {
            client.postChatMessage(
              channel,
              s"<@${user}>, I have an answer for `${convo.triggerMessage}`:",
              asUser = Some(true)
            )
          } else {
            Future.successful({})
          }
        }.getOrElse(Future.successful({}))
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
      } yield {}
    } else {
      Future.successful({})
    }
  }

  def sendMessageSegmentsInOrder(
                           segments: List[String],
                           channelToUse: String,
                           maybeShouldUnfurl: Option[Boolean],
                           maybeAttachments: Option[Seq[Attachment]],
                           maybeConversation: Option[Conversation],
                           maybePreviousTs: Option[String]
                         ): Future[Option[String]] = {
    if (segments.isEmpty) {
      Future.successful(maybePreviousTs)
    } else {
      val segment = segments.head.trim
      // Slack API gives an error for empty messages
      if (segment.isEmpty) {
        Future.successful(None)
      } else {
        val maybeAttachmentsForSegment = if (segments.tail.isEmpty) {
          maybeAttachments
        } else {
          None
        }
        val maybeThreadTsToUse = maybeConversation.filterNot(_.state == Conversation.DONE_STATE).flatMap(_.maybeThreadId)
        val replyBroadcast = maybeThreadTsToUse.isDefined && !isDirectMessage(channelToUse) && maybeConversation.exists(_.state == Conversation.DONE_STATE)
        client.postChatMessage(
          channelToUse,
          segment,
          asUser = Some(true),
          unfurlLinks = maybeShouldUnfurl,
          unfurlMedia = Some(true),
          attachments = maybeAttachmentsForSegment,
          threadTs = maybeThreadTsToUse,
          replyBroadcast = Some(replyBroadcast)
        )
      }.flatMap { ts => sendMessageSegmentsInOrder(segments.tail, channelToUse, maybeShouldUnfurl, maybeAttachments, maybeConversation, Some(ts))}
    }
  }

  def sendMessage(
                   unformattedText: String,
                   forcePrivate: Boolean,
                   maybeShouldUnfurl: Option[Boolean],
                   maybeConversation: Option[Conversation],
                   maybeActions: Option[MessageActions] = None
                 )(implicit ec: ExecutionContext): Future[Option[String]] = {
    val formattedText = SlackMessageFormatter.bodyTextFor(unformattedText)
    val maybeAttachments = maybeActions.flatMap { actions =>
      actions match {
        case a: SlackMessageActions => Some(Seq(a.attachment))
        case _ => None
      }
    }
    for {
      channelToUse <- channelForSend(forcePrivate, maybeConversation)
      _ <- sendPreamble(formattedText, channelToUse, maybeConversation)
      maybeLastTs <- sendMessageSegmentsInOrder(messageSegmentsFor(formattedText), channelToUse, maybeShouldUnfurl, maybeAttachments, maybeConversation, None)
    } yield maybeLastTs
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

  override def botPrefix: String = s"<@${profile.userId}> "

}

object SlackMessageEvent {

  def mentionRegexFor(botId: String): Regex = s"""<@$botId>""".r
  def toBotRegexFor(botId: String): Regex = s"""^<@$botId>:?\\s*""".r

  // From Slack docs:
  //
  // "For best results, message bodies should contain no more than a few thousand characters."
  val MAX_MESSAGE_LENGTH = 2000
}
