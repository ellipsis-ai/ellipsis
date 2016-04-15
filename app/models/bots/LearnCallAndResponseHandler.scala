package models.bots

import models._
import slack.models.Message
import slack.rtm.SlackRtmClient
import scala.util.matching.Regex
import scala.concurrent.ExecutionContext.Implicits.global


case class LearnCallAndResponseHandler(
                                        client: SlackRtmClient,
                                        profile: SlackBotProfile,
                                        message: Message
                                        ) extends BotHandler {

  def run = {
    val regex = LearnCallAndResponseHandler.regex
    val regex(call, response) = message.text
    val action = Response.ensure(profile.teamId, call, response).map { _ =>
      client.sendMessage(message.channel, s"<@${message.user}>: Got it! I'll say $response when someone says $call")
    }
    Models.runNow(action)
  }
}

object LearnCallAndResponseHandler extends BotHandlerType {

  type T = LearnCallAndResponseHandler

  def regex: Regex = """.*when\s*(.+)\s*say\s*(.+)""".r
}
