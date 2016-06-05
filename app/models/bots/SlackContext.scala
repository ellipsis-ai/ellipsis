package models.bots

import models.accounts.SlackBotProfile
import models.bots.conversations.Conversation
import slack.models.Message
import slack.rtm.SlackRtmClient

import scala.concurrent.ExecutionContext
import scala.util.matching.Regex

case class SlackContext(
                        client: SlackRtmClient,
                        profile: SlackBotProfile,
                        message: Message
                        ) extends MessageContext {

  lazy val botId: String = client.state.self.id
  lazy val name: String = Conversation.SLACK_CONTEXT
  lazy val userIdForContext: String = message.user

  lazy val isDirectMessage: Boolean = {
    message.channel.startsWith("D")
  }

  lazy val isToBot: Boolean = {
    isDirectMessage || toBotRegex.findFirstMatchIn(message.text).nonEmpty
  }

  lazy val toBotRegex: Regex = s"""^<@$botId>:?\\s*""".r

  lazy val relevantMessageText: String = toBotRegex.replaceFirstIn(message.text, "")

  def sendMessage(text: String)(implicit ec: ExecutionContext): Unit = {
    client.apiClient.postChatMessage(message.channel, text)
  }
}
