package models.bots.handlers

import models._
import models.accounts.SlackBotProfile
import models.bots.LinkShortcut
import slack.models.Message
import slack.rtm.SlackRtmClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.matching.Regex


case class LearnGoHandler(
                      client: SlackRtmClient,
                      profile: SlackBotProfile,
                      message: Message
                      ) extends BotHandler {

  def run = {
    val regex = """.*when\s+(.+)\s+go\s<([^\|]+)\|?.*>""".r
    val regex(label, link) = message.text
    println(message.text)
    println(link)
    val action = LinkShortcut(label.trim, link.trim, profile.teamId).save.map { _ =>
      client.sendMessage(message.channel, s"<@${message.user}>: Got it! I'll link to $link when someone says `@ellipsis: go $label`")
    }
    Models.runNow(action)
  }
}

object LearnGoHandler extends BotHandlerType {

  type T = LearnGoHandler

  def regex: Regex = """.*when\s+(.+)\s+go\s+(.+)""".r
}
