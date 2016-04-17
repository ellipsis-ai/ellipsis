package models.bots

import models.SlackBotProfile
import slack.models.Message
import slack.rtm.SlackRtmClient

import scala.util.matching.Regex

trait BotHandler {
  val client: SlackRtmClient
  val profile: SlackBotProfile
  val message: Message

  def run: Unit
}

trait BotHandlerType {
  type T <: BotHandler
  def apply(client: SlackRtmClient, profile: SlackBotProfile, message: Message): T
  def regex: Regex
  def canHandle(message: Message): Boolean = {
    regex.findFirstMatchIn(message.text).nonEmpty
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
