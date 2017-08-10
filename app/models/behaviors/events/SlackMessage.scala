package models.behaviors.events

import models.SlackMessageFormatter
import models.accounts.slack.SlackUserInfo

case class SlackMessage(originalText: String, withoutBotPrefix: String, unformattedText: String)

object SlackMessage {
  def removeBotPrefix(text: String, botUserId: String): String = {
    val withoutDotDotDot = MessageEvent.ellipsisRegex.replaceFirstIn(text, "")
    SlackMessageEvent.toBotRegexFor(botUserId).replaceFirstIn(withoutDotDotDot, "")
  }

  def fromUnformattedText(text: String, botUserId: String): SlackMessage = {
    val withoutBotPrefix = removeBotPrefix(text, botUserId)
    SlackMessage(text, withoutBotPrefix, withoutBotPrefix)
  }

  def fromFormattedText(text: String, botUserId: String, slackUserList: Seq[SlackUserInfo]): SlackMessage = {
    val withoutBotPrefix = removeBotPrefix(text, botUserId)
    SlackMessage(text, withoutBotPrefix, SlackMessageFormatter.unformatText(withoutBotPrefix, slackUserList))
  }

  def blank: SlackMessage = SlackMessage("", "", "")
}
