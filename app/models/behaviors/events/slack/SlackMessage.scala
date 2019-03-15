package models.behaviors.events.slack

import json.SlackUserData
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.events.MessageEvent
import services.DefaultServices
import services.slack.SlackEventService

import scala.concurrent.{ExecutionContext, Future}

case class SlackMessage(originalText: String, withoutBotPrefix: String, unformattedText: String, userList: Set[SlackUserData], maybeTs: Option[String])

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

  def augmentUserIdsWithNames(initialText: String, userList: Set[SlackUserData]): String = {
    userList.foldLeft(initialText) { (resultText, user) =>
      resultText.replace(s"""<@${user.accountId}>""", s"""<@${user.accountId}|${user.getDisplayName}>""")
    }
  }

  def unformatText(text: String): String = {
    unescapeSlackHTMLEntities(unformatLinks(text))
  }

  def unformatTextWithUsers(text: String, userList: Set[SlackUserData]): String = {
    unformatText(augmentUserIdsWithNames(text, userList))
  }

  def removeBotPrefix(text: String, botProfile: SlackBotProfile): String = {
    val withoutShortcut = if (botProfile.allowShortcutMention) {
      MessageEvent.ellipsisShortcutMentionRegex.replaceFirstIn(text, "")
    } else {
      text
    }
    SlackMessageEvent.toBotRegexFor(botProfile.userId).replaceFirstIn(withoutShortcut, "")
  }

  def fromUnformattedText(text: String, botProfile: SlackBotProfile, maybeTs: Option[String]): SlackMessage = {
    val withoutBotPrefix = removeBotPrefix(text, botProfile)
    SlackMessage(text, withoutBotPrefix, withoutBotPrefix, Set.empty[SlackUserData], maybeTs)
  }

  def userIdsInText(text: String): Set[String] = {
    """<@(\w+)>""".r.findAllMatchIn(text).flatMap(_.subgroups).toSet
  }

  def fromFormattedText(text: String, botProfile: SlackBotProfile, slackEventService: SlackEventService, maybeTs: Option[String])(implicit ec: ExecutionContext): Future[SlackMessage] = {
    val withoutBotPrefix = removeBotPrefix(text, botProfile)
    val userList = userIdsInText(text)
    for {
      slackUsers <- slackEventService.slackUserDataList(userList, botProfile)
    } yield {
      SlackMessage(text, withoutBotPrefix, unformatTextWithUsers(withoutBotPrefix, slackUsers), slackUsers, maybeTs)
    }
  }

  def maybeFromMessageTs(
                          ts: String,
                          channel: String,
                          botProfile: SlackBotProfile,
                          services: DefaultServices
                        )(implicit ec: ExecutionContext): Future[Option[SlackMessage]] = {
    val client = services.slackApiService.clientFor(botProfile)
    client.findReaction(channel, ts, services.slackEventService)
  }

  def blank: SlackMessage = SlackMessage("", "", "", Set.empty[SlackUserData], None)
}
