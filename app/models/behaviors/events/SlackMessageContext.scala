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
import slack.models.Message
import slack.rtm.SlackRtmClient

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.matching.Regex

case class SlackMessageContext(
                        client: SlackRtmClient,
                        profile: SlackBotProfile,
                        message: Message
                        ) extends MessageContext {

  val fullMessageText = message.text

  val teamId: String = profile.teamId

  lazy val botId: String = client.state.self.id
  lazy val name: String = Conversation.SLACK_CONTEXT
  override val conversationContext = Conversation.SLACK_CONTEXT ++ "#" ++ message.channel
  lazy val userIdForContext: String = message.user

  lazy val isDirectMessage: Boolean = {
    message.channel.startsWith("D")
  }

  override def relevantMessageText: String = {
    SlackMessageContext.toBotRegexFor(botId).replaceFirstIn(super.relevantMessageText, "")
  }

  lazy val includesBotMention: Boolean = {
    isDirectMessage ||
      SlackMessageContext.mentionRegexFor(botId).findFirstMatchIn(message.text).nonEmpty ||
      MessageContext.ellipsisRegex.findFirstMatchIn(message.text).nonEmpty
  }

  lazy val isResponseExpected: Boolean = includesBotMention

  private def messageSegmentsFor(formattedText: String): Seq[String] = {
    if (formattedText.length < SlackMessageContext.MAX_MESSAGE_LENGTH) {
      Seq(formattedText)
    } else {
      val largestPossibleSegment = formattedText.substring(0, SlackMessageContext.MAX_MESSAGE_LENGTH)
      val lastNewlineIndex = Math.max(largestPossibleSegment.lastIndexOf('\n'), largestPossibleSegment.lastIndexOf('\r'))
      val lastIndex = if (lastNewlineIndex < 0) { SlackMessageContext.MAX_MESSAGE_LENGTH - 1 } else { lastNewlineIndex }
      Seq(formattedText.substring(0, lastIndex)) ++ messageSegmentsFor(formattedText.substring(lastIndex + 1))
    }
  }

  def sendMessage(unformattedText: String, forcePrivate: Boolean, maybeShouldUnfurl: Option[Boolean] = None)(implicit ec: ExecutionContext): Unit = {
    val formattedText = SlackMessageFormatter(client).bodyTextFor(unformattedText)
    val apiClient = client.apiClient
    val maybeDMChannel = if (forcePrivate) {
      apiClient.listIms.find(_.user == message.user).map(_.id)
    } else {
      None
    }
    messageSegmentsFor(formattedText).foreach { ea =>
      // The Slack API considers sending an empty message to be an error rather than a no-op
      if (ea.nonEmpty) {
        if (maybeDMChannel.isDefined && !maybeDMChannel.contains(message.channel)) {
          apiClient.postChatMessage(message.channel, s"<@${message.user}> I've sent you a private message :sleuth_or_spy:", asUser = Some(true), unfurlLinks = maybeShouldUnfurl, unfurlMedia = maybeShouldUnfurl)
        }
        apiClient.postChatMessage(maybeDMChannel.getOrElse(message.channel), ea, asUser = Some(true), unfurlLinks = maybeShouldUnfurl, unfurlMedia = maybeShouldUnfurl)
      }
    }
  }

  override def recentMessages(dataService: DataService): Future[Seq[String]] = {
    for {
      maybeTeam <- dataService.teams.find(profile.teamId)
      maybeOAuthToken <- dataService.oauth2Tokens.maybeFullForSlackTeamId(profile.slackTeamId)
      maybeUserClient <- Future.successful(maybeOAuthToken.map { token =>
        SlackApiClient(token.accessToken)
      })
      maybeHistory <- maybeUserClient.map { userClient =>
        userClient.getChannelHistory(message.channel, latest = Some(message.ts)).map(Some(_))
      }.getOrElse(Future.successful(None))
      messages <- Future.successful(maybeHistory.map { history =>
        history.messages.slice(0, 10).reverse.flatMap { json =>
          (json \ "text").asOpt[String]
        }
      }.getOrElse(Seq()))
    } yield messages
  }

  def maybeOngoingConversation(dataService: DataService): Future[Option[Conversation]] = {
    dataService.conversations.findOngoingFor(message.user, conversationContext)
  }

  override def ensureUser(dataService: DataService)(implicit ec: ExecutionContext): Future[User] = {
    super.ensureUser(dataService).flatMap { user =>
      dataService.slackProfiles.save(SlackProfile(profile.slackTeamId, loginInfo)).map(_ => user)
    }
  }

  override def detailsFor(ws: WSClient, dataService: DataService): Future[JsObject] = {
    Future {
      client.apiClient.getUserInfo(userIdForContext)
    }.map { user =>
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
}

object SlackMessageContext {

  def mentionRegexFor(botId: String): Regex = s"""<@$botId>""".r
  def toBotRegexFor(botId: String): Regex = s"""^<@$botId>:?\\s*""".r

  // From Slack docs:
  //
  // "For best results, message bodies should contain no more than a few thousand characters."
  val MAX_MESSAGE_LENGTH = 2000
}
