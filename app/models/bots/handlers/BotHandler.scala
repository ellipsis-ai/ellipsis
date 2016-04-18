package models.bots.handlers

import models.accounts.SlackBotProfile
import slack.models.Message
import slack.rtm.SlackRtmClient

import scala.util.matching.Regex

trait BotHandler {
  val client: SlackRtmClient
  val profile: SlackBotProfile
  val message: Message

  def run: Unit
  def botId: String = client.state.self.id
}

trait BotHandlerType {

  type T <: BotHandler
  def apply(client: SlackRtmClient, profile: SlackBotProfile, message: Message): T
  def regex(botId: String): Regex
  def canHandle(message: Message, botId: String): Boolean = {
    regex(botId).findFirstMatchIn(message.text).nonEmpty
  }
  def runWith(client: SlackRtmClient, profile: SlackBotProfile, message: Message): Unit = {
    apply(client, profile, message).run
  }
}

object BotHandler {
  def ordered: Seq[BotHandlerType] = Seq(
    LearnCallAndResponseHandler,
    LearnGoHandler,
    GoHandler,
    CallAndResponseHandler
  )
}
