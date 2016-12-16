package models.behaviors.events

import models.SlackMessageFormatter
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.conversations.conversation.Conversation
import services.DataService
import slack.rtm.SlackRtmClient

import scala.concurrent.{ExecutionContext, Future}

case class APIMessageContext(
                              client: SlackRtmClient,
                              profile: SlackBotProfile,
                              channel: String,
                              messageText: String
                              ) extends MessageContext {

  val teamId: String = profile.teamId

  val fullMessageText = messageText

  lazy val name: String = Conversation.API_CONTEXT
  lazy val maybeChannel = Some(channel)
  lazy val userIdForContext: String = name

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
