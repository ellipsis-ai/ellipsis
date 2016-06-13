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

  lazy val relevantMessageText: String = SlackContext.toBotRegexFor(botId).replaceFirstIn(message.text, "")

  // either a DM to the bot or explicitly mentions bot
  lazy val includesBotMention: Boolean = {
    isDirectMessage || SlackContext.mentionRegexFor(botId).findFirstMatchIn(message.text).nonEmpty
  }

  lazy val isResponseExpected: Boolean = includesBotMention

  def sendMessage(text: String)(implicit ec: ExecutionContext): Unit = {
    client.apiClient.postChatMessage(message.channel, text)
  }
}

object SlackContext {

  def mentionRegexFor(botId: String): Regex = s"""<@$botId>""".r
  def toBotRegexFor(botId: String): Regex = s"""^<@$botId>:?\\s*""".r
}
