package models.behaviors.events

import json.SlackUserData
import models.accounts.slack.botprofile.SlackBotProfile
import services.SlackEventService

import scala.concurrent.{ExecutionContext, Future}

case class SlackMessage(originalText: String, withoutBotPrefix: String, unformattedText: String, userList: Set[SlackUserData])

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

  def removeBotPrefix(text: String, botUserId: String): String = {
    val withoutDotDotDot = MessageEvent.ellipsisRegex.replaceFirstIn(text, "")
    SlackMessageEvent.toBotRegexFor(botUserId).replaceFirstIn(withoutDotDotDot, "")
  }

  def fromUnformattedText(text: String, botUserId: String): SlackMessage = {
    val withoutBotPrefix = removeBotPrefix(text, botUserId)
    SlackMessage(text, withoutBotPrefix, withoutBotPrefix, Set.empty[SlackUserData])
  }

  def userIdsInText(text: String): Set[String] = {
    """<@(\w+)>""".r.findAllMatchIn(text).flatMap(_.subgroups).toSet
  }

  def fromFormattedText(text: String, botProfile: SlackBotProfile, slackEventService: SlackEventService)(implicit ec: ExecutionContext): Future[SlackMessage] = {
    val withoutBotPrefix = removeBotPrefix(text, botProfile.userId)
    val userList = userIdsInText(text)
    for {
      slackUsers <- slackEventService.slackUserDataList(userList, botProfile)
    } yield {
      SlackMessage(text, withoutBotPrefix, unformatTextWithUsers(withoutBotPrefix, slackUsers), slackUsers)
    }
  }

  def blank: SlackMessage = SlackMessage("", "", "", Set.empty[SlackUserData])
}
