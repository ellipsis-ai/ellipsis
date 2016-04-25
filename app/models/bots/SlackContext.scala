package models.bots

import models.accounts.SlackBotProfile
import slack.models.Message
import slack.rtm.SlackRtmClient

case class SlackContext(
                        client: SlackRtmClient,
                        profile: SlackBotProfile,
                        message: Message
                        ) extends MessageContext {

  def sendMessage(text: String): Unit = {
    client.sendMessage(message.channel, s"<@${message.user}>: $text")
  }
}
