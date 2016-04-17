package models.bots.handlers

import models._
import models.accounts.SlackBotProfile
import models.bots.{Response, Call}
import slack.models.Message
import slack.rtm.SlackRtmClient
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.matching.Regex


case class CallAndResponseHandler(
                                        client: SlackRtmClient,
                                        profile: SlackBotProfile,
                                        message: Message
                                        ) extends BotHandler {

  def run = {
    val messageTextRegex = """<@\w+>:\s(.*)""".r
    val messageTextRegex(messageText) = message.text
    val action = for {
      maybeCall <- Call.matchFor(messageText.trim, profile.teamId)
      maybeResponse <- maybeCall.map { call =>
        Response.findByCallId(call.id)
      }.getOrElse(DBIO.successful(None))
    } yield {
        maybeResponse.map { response =>
          client.sendMessage(message.channel, s"<@${message.user}>: ${response.text}")
        }.getOrElse {
          client.sendMessage(message.channel, s"<@${message.user}>: I don't know what you're talking about")
        }
      }

    Models.runNow(action)
  }
}

object CallAndResponseHandler extends BotHandlerType {

  type T = CallAndResponseHandler

  def regex: Regex = ".*".r
}
