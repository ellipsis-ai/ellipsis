package models.behaviors.events

import models.SlackMessageFormatter
import models.accounts.slack.SlackUserInfo
import models.accounts.slack.botprofile.SlackBotProfile
import services.SlackEventService

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

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

  def fromFormattedTextWithUsers(text: String, botUserId: String, slackUserList: Seq[SlackUserInfo]): SlackMessage = {
    val withoutBotPrefix = removeBotPrefix(text, botUserId)
    SlackMessage(text, withoutBotPrefix, SlackMessageFormatter.unformatTextWithUsers(withoutBotPrefix, slackUserList))
  }

  def textContainsRawUserIds(text: String): Boolean = {
    """<@\w+>""".r.findFirstIn(text).isDefined
  }

  def fromFormattedText(text: String, botProfile: SlackBotProfile, slackEventService: SlackEventService): Future[SlackMessage] = {
    val withoutBotPrefix = removeBotPrefix(text, botProfile.userId)

    for {
      maybeSlackUserList <- if (textContainsRawUserIds(withoutBotPrefix)) {
        slackEventService.maybeSlackUserListFor(botProfile)
      } else {
        Future.successful(None)
      }
    } yield {
      maybeSlackUserList.map { slackUserList =>
        SlackMessage(text, withoutBotPrefix, SlackMessageFormatter.unformatTextWithUsers(withoutBotPrefix, slackUserList))
      }.getOrElse {
        // TODO: What should we do if a message contains users but the slack user list request failed?
        SlackMessage(text, withoutBotPrefix, SlackMessageFormatter.unformatText(withoutBotPrefix))
      }
    }
  }

  def blank: SlackMessage = SlackMessage("", "", "")
}
