package models.bots.handlers

import models._
import models.accounts.SlackBotProfile
import models.bots.Response
import slack.models.Message
import slack.rtm.SlackRtmClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.matching.Regex


case class LearnCallAndResponseHandler(
                                        client: SlackRtmClient,
                                        profile: SlackBotProfile,
                                        message: Message
                                        ) extends BotHandler {

  def run = {
    val regex = LearnCallAndResponseHandler.regex(botId)
    val regex(call, response) = message.text
    val action = Response.ensure(profile.teamId, call, response).map { _ =>
      client.sendMessage(message.channel, s"<@${message.user}>: Got it! I'll say $response when someone says $call")
    }
    Models.runNow(action)
  }
}

object LearnCallAndResponseHandler extends BotHandlerType {

  type T = LearnCallAndResponseHandler

  def regex(botId: String): Regex = s"""<@${botId}>:\\s+when\\s+(.+)\\s+say\\s+(.+)""".r
}
