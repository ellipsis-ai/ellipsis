package models

import json.UserData
import models.behaviors.templates.{ChatPlatformRenderer, MSTeamsRenderer}

object MSTeamsMessageFormatter extends ChatPlatformMessageFormatter {
  def newRendererFor(builder: StringBuilder): ChatPlatformRenderer = new MSTeamsRenderer(builder)

  def convertUsernamesToLinks(formattedText: String, userList: Set[UserData]): String = {
    userList.toSeq.foldLeft(formattedText) { (text, user) =>
      user.userIdForContext.map { uid =>
        val linkRegex = raw"""<@$uid>""".r
        val replacement = user.formattedLink.getOrElse {
          s"@${user.userNameOrDefault}"
        }
        linkRegex.replaceAllIn(text, replacement)
      }.getOrElse(text)
    }
  }
}
