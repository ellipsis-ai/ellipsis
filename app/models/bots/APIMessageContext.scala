package models.bots

import models.accounts.SlackBotProfile
import models.bots.conversations.Conversation
import slack.rtm.SlackRtmClient
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext

case class APIMessageContext(
                              client: SlackRtmClient,
                              profile: SlackBotProfile,
                              channel: String,
                              messageText: String
                              ) extends MessageContext {

  val teamId: String = profile.teamId

  val fullMessageText = messageText

  lazy val name: String = Conversation.API_CONTEXT
  lazy val userIdForContext: String = name

  val includesBotMention: Boolean = true

  lazy val isResponseExpected: Boolean = includesBotMention

  def sendMessage(unformattedText: String)(implicit ec: ExecutionContext): Unit = {
    val formattedText = SlackMessageFormatter(client).bodyTextFor(unformattedText)
    // The Slack API considers sending an empty message to be an error rather than a no-op
    if (formattedText.nonEmpty) {
      client.apiClient.postChatMessage(channel, formattedText, asUser = Some(true))
    }
  }

  def maybeOngoingConversation: DBIO[Option[Conversation]] = DBIO.successful(None)
}
