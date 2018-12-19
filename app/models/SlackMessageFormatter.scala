package models

import java.util.regex.Matcher

import json.UserData
import models.behaviors.templates.{ChatPlatformRenderer, SlackRenderer}

object SlackMessageFormatter extends ChatPlatformMessageFormatter {
  def newRendererFor(builder: StringBuilder): ChatPlatformRenderer = new SlackRenderer(builder)

  def convertUsernamesToLinks(formattedText: String, userList: Set[UserData]): String = {
    userList.toSeq.foldLeft(formattedText) { (text, user) =>
      val userNameLinkRegex = raw"""(^|\s|\W)<?@\Q${user.userNameOrDefault}\E>?($$|\s|\W)""".r
      val replacement = user.userIdForContext.map { userId =>
        s"<@${userId}>"
      }.getOrElse {
        s"@${user.userNameOrDefault}"
      }
      userNameLinkRegex.replaceAllIn(text, s"$$1${Matcher.quoteReplacement(replacement)}$$2")
    }
  }
}
