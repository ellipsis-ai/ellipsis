package models.bots

import models.accounts.SlackBotProfile
import models.bots.conversations.Conversation
import slack.models.Message
import slack.rtm.SlackRtmClient

import scala.concurrent.ExecutionContext

case class SlackContext(
                        client: SlackRtmClient,
                        profile: SlackBotProfile,
                        message: Message
                        ) extends MessageContext {

  def sendMessage(text: String)(implicit ec: ExecutionContext): Unit = {
    client.apiClient.postChatMessage(message.channel, s"<@${message.user}>: $text")
//    postChatMessage() uses formatting
//    client.sendMessage(message.channel, s"<@${message.user}>: $text")
  }

  val name: String = Conversation.SLACK_CONTEXT
  def userIdForContext: String = message.user
}
