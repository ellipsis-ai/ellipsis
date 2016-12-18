package models.behaviors.events

import models.SlackMessageFormatter
import models.accounts.slack.botprofile.SlackBotProfile
import models.accounts.slack.profile.SlackProfile
import models.behaviors.conversations.conversation.Conversation
import services.DataService
import slack.rtm.SlackRtmClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

case class APIMessageContext(
                              client: SlackRtmClient,
                              profile: SlackBotProfile,
                              channel: String,
                              messageText: String,
                              maybeSlackProfile: Option[SlackProfile]
                              ) extends MessageContext {

  val teamId: String = profile.teamId

  val fullMessageText = messageText

  lazy val name: String = maybeSlackProfile.map(_.loginInfo.providerID).getOrElse(Conversation.API_CONTEXT)
  lazy val maybeChannel = Some(channel)
  def maybeDMChannel = {
    maybeSlackProfile.flatMap { slackProfile =>
      client.apiClient.listIms.find(_.user == slackProfile.loginInfo.providerKey).map(_.id)
    }
  }
  lazy val userIdForContext: String = maybeSlackProfile.map(_.loginInfo.providerKey).getOrElse(name)

  val includesBotMention: Boolean = true

  lazy val isResponseExpected: Boolean = includesBotMention

  def sendMessage(
                   unformattedText: String,
                   forcePrivate: Boolean,
                   maybeShouldUnfurl: Option[Boolean],
                   maybeConversation: Option[Conversation]
                 )(implicit ec: ExecutionContext): Unit = {
    val formattedText = SlackMessageFormatter(client).bodyTextFor(unformattedText)
    // The Slack API considers sending an empty message to be an error rather than a no-op
    if (formattedText.nonEmpty) {
      client.apiClient.postChatMessage(channel, formattedText, asUser = Some(true))
    }
  }

  def maybeOngoingConversation(dataService: DataService): Future[Option[Conversation]] = Future.successful(None)
}
