package models.bots.handlers

import models._
import models.accounts.SlackBotProfile
import models.bots.LinkShortcut
import slack.models.Message
import slack.rtm.SlackRtmClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.matching.Regex


case class GoHandler(
                     client: SlackRtmClient,
                     profile: SlackBotProfile,
                     message: Message
                     ) extends BotHandler {

  def run = {
    val messageTextRegex = GoHandler.regex
    val messageTextRegex(shortcutLabel) = message.text
    val action = for {
      maybeShortcut <- LinkShortcut.find(shortcutLabel.trim, profile.teamId)
    } yield {
        maybeShortcut.map { shortcut =>
          client.sendMessage(message.channel, s"<@${message.user}>: :point_right: ${shortcut.link} :point_left:")
        }.getOrElse {
          client.sendMessage(message.channel, s"<@${message.user}>: I don't know where to go for `go ${shortcutLabel}`")
        }
      }

    Models.runNow(action)
  }
}

object GoHandler extends BotHandlerType {

  type T = GoHandler

  def regex: Regex = """.*go\s+(.+)""".r
}
