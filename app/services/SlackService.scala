package services

import javax.inject._

import models._
import models.accounts.{SlackBotProfile, SlackBotProfileQueries}
import models.bots._
import play.api.i18n.MessagesApi
import play.api.inject.ApplicationLifecycle
import slack.rtm.SlackRtmClient
import akka.actor.ActorSystem
import models.bots.events.{EventHandler, SlackMessageContext, SlackMessageEvent}
import slick.driver.PostgresDriver.api._

import scala.concurrent.{Future, Promise}
import scala.concurrent.duration._

@Singleton
class SlackService @Inject() (
                               appLifecycle: ApplicationLifecycle,
                               val models: Models,
                               messages: MessagesApi,
                               val eventHandler: EventHandler
                               ) {

  implicit val system = ActorSystem("slack")
  implicit val ec = system.dispatcher

  val clients = scala.collection.mutable.Map.empty[SlackBotProfile, SlackRtmClient]

  start

  appLifecycle.addStopHook { () =>
    Future.successful(stop)
  }

  def stopFor(profile: SlackBotProfile): Unit = {

    println(s"stopping client for ${profile.userId}")

    clients.get(profile).foreach { client =>
      client.close()
    }
    clients.remove(profile)
  }

  def startFor(profile: SlackBotProfile) {

    stopFor(profile)

    println(s"starting client for ${profile.userId}")
    val client = SlackRtmClient(profile.token, 5.seconds)
    clients.put(profile, client)
    val selfId = client.state.self.id

    client.onMessage { message =>
      if (message.user != selfId) {
        val p = Promise[BehaviorResult]()
        val handleMessage = eventHandler.handle(SlackMessageEvent(SlackMessageContext(client, profile, message)))
        p.completeWith(handleMessage)
        val indicateTyping = Future {
          Thread.sleep(500)
          while (!p.isCompleted) {
            client.indicateTyping(message.channel)
            Thread.sleep(3000)
          }
        }
      }
    }

  }

  private def allProfiles: DBIO[Seq[SlackBotProfile]] = {
    SlackBotProfileQueries.all.result
  }

  def start: Future[Any] = {
    stop
    val action = allProfiles.map { profiles =>
      profiles.foreach { profile =>
        startFor(profile)
      }
    }
    models.run(action)
  }

  def stop = {
    clients.clone().foreach { case(profile, client) =>
      client.close
      clients.remove(profile)
    }
  }

}
