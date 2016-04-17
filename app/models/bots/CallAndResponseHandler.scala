package models.bots

import models._
import models.accounts.SlackBotProfile
import slack.models.Message
import slack.rtm.SlackRtmClient
import scala.util.matching.Regex
import scala.concurrent.ExecutionContext.Implicits.global
import slick.driver.PostgresDriver.api._


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
