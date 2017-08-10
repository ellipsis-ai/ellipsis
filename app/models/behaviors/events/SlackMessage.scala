package models.behaviors.events

import models.accounts.slack.SlackUserInfo
import models.accounts.slack.botprofile.SlackBotProfile
import services.SlackEventService

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

case class SlackMessage(originalText: String, withoutBotPrefix: String, unformattedText: String)

object SlackMessage {
  def unformatLinks(text: String): String = {
    text.
      replaceAll("""<@(?:.+?\|)?(.+?)>""", "@$1").
      replaceAll("""<#(?:.+?\|)?(.+?)>""", "#$1").
      replaceAll("""<!(here|group|channel|everyone)(\|(here|group|channel|everyone))?>""", "@$1").
      replaceAll("""<!subteam\^.+?\|(.+?)>""", "@$1").
      replaceAll("""<!date.+?\|(.+?)>""", "$1").
      replaceAll("""<(?:[^!].*?\|)(.+?)>""", "$1").
      replaceAll("""<([^!].*?)>""", "$1").
      replaceAll("""<!(?:.+?\|)?(.+?)>""", "<$1>")
  }

  def unescapeSlackHTMLEntities(text: String): String = {
    text.replaceAll("&amp;", "&").replaceAll("&lt;", "<").replaceAll("&gt;", ">")
  }

  def augmentUserIdsWithNames(initialText: String, userList: Seq[SlackUserInfo]): String = {
    userList.foldLeft(initialText) { (resultText, user) =>
      resultText.replace(s"""<@${user.userId}>""", s"""<@${user.userId}|${user.name}>""")
    }
  }

  def unformatText(text: String): String = {
    unescapeSlackHTMLEntities(unformatLinks(text))
  }

  def unformatTextWithUsers(text: String, userList: Seq[SlackUserInfo]): String = {
    unformatText(augmentUserIdsWithNames(text, userList))
  }

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
    SlackMessage(text, withoutBotPrefix, unformatTextWithUsers(withoutBotPrefix, slackUserList))
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
        SlackMessage(text, withoutBotPrefix, unformatTextWithUsers(withoutBotPrefix, slackUserList))
      }.getOrElse {
        // TODO: What should we do if a message contains users but the slack user list request failed?
        SlackMessage(text, withoutBotPrefix, unformatText(withoutBotPrefix))
      }
    }
  }

  def blank: SlackMessage = SlackMessage("", "", "")
}
