package services

import models._
import models.accounts.{SlackBotProfileQueries, SlackBotProfile}
import models.bots.handlers.BotHandler
import slack.rtm.SlackRtmClient
import akka.actor.ActorSystem
import slick.driver.PostgresDriver.api._
import scala.concurrent.Future
import scala.concurrent.duration._

object SlackService {

  implicit val system = ActorSystem("slack")
  implicit val ec = system.dispatcher

  def startFor(profile: SlackBotProfile) {

    val client = SlackRtmClient(profile.token, 5.seconds)
    val selfId = client.state.self.id

    client.onMessage { message =>
      if (message.user != selfId) {
        BotHandler.ordered.find(_.canHandle(message)).foreach(_.runWith(client, profile, message))
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
