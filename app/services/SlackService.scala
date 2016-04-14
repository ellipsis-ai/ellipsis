package services

import models._
import slack.models.Message
import slack.rtm.SlackRtmClient
import slack.SlackUtil
import akka.actor.ActorSystem
import slick.driver.PostgresDriver.api._
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.matching.Regex

object SlackService {

  implicit val system = ActorSystem("slack")
  implicit val ec = system.dispatcher

  private def processLearnMessage(message: Message, client: SlackRtmClient, learnRegex: Regex, profile: SlackBotProfile) = {
    val learnRegex(call, response) = message.text
    val action = Response.ensure(profile.teamId, call, response).map { _ =>
      client.sendMessage(message.channel, s"<@${message.user}>: Got it! I'll say $response when someone says $call")
    }
    Models.runNow(action)
  }

  private def processOtherMessage(message: Message, client: SlackRtmClient, profile: SlackBotProfile) = {
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

  def startFor(profile: SlackBotProfile) {

    val client = SlackRtmClient(profile.token, 5.seconds)
    val selfId = client.state.self.id

    client.onMessage { message =>

      if (message.user != selfId) {
        val learnRegex = """.*when\s*(\w+)\s*say\s*(\w+)""".r
        if (learnRegex.findAllIn(message.text).nonEmpty) {
          processLearnMessage(message, client, learnRegex, profile)
        } else {
          processOtherMessage(message, client, profile)
        }
      }

    }

  }

  private def allProfiles: DBIO[Seq[SlackBotProfile]] = {
    SlackBotProfileQueries.all.result
  }

  def start: Future[Any] = {
    val action = allProfiles.map { profiles =>
      profiles.foreach { profile =>
        startFor(profile)
      }
    }
    Models.run(action)
  }

  def closeFor(profile: SlackBotProfile): Unit = {
    val client = SlackRtmClient(profile.token)
    client.close
  }

  def stop: Future[Any] = {
    val action = allProfiles.map { profiles =>
      profiles.foreach { profile =>
        closeFor(profile)
      }
    }
    Models.run(action)
  }

}
