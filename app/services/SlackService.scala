package services

import models._
import slack.rtm.SlackRtmClient
import slack.SlackUtil
import akka.actor.ActorSystem
import slick.driver.PostgresDriver.api._
import scala.concurrent.Future
import scala.concurrent.duration._

object SlackService {

  implicit val system = ActorSystem("slack")
  implicit val ec = system.dispatcher

  def startFor(token: String) {

    val client = SlackRtmClient(token, 5.seconds)
    val selfId = client.state.self.id

    client.onMessage { message =>
      val mentionedIds = SlackUtil.extractMentionedIds(message.text)

      if (mentionedIds.contains(selfId)) {
        client.sendMessage(message.channel, s"<@${message.user}>: Hey!")
      }
    }
  }

  private def allAccessTokens: DBIO[Seq[String]] = {
    for {
      slackBotProfiles <- SlackBotProfileQueries.all.result
    } yield slackBotProfiles.map(_.token)
  }

  def start: Future[Any] = {
    val action = allAccessTokens.map { tokens =>
      tokens.foreach { token =>
        startFor(token)
      }
    }
    Models.run(action)
  }

  def closeFor(token: String): Unit = {
    val client = SlackRtmClient(token)
    client.close
  }

  def stop: Future[Any] = {
    val action = allAccessTokens.map { tokens =>
      tokens.foreach { token =>
        closeFor(token)
      }
    }
    Models.run(action)
  }

}
