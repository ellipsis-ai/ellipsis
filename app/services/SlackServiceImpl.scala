package services

import javax.inject._

import play.api.i18n.MessagesApi
import play.api.inject.ApplicationLifecycle
import slack.rtm.SlackRtmClient
import akka.actor.ActorSystem
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.events.{EventHandler, SlackMessageContext, SlackMessageEvent}

import scala.concurrent.{Future, Promise}
import scala.concurrent.duration._

@Singleton
class SlackServiceImpl @Inject() (
                               appLifecycle: ApplicationLifecycle,
                               val dataService: DataService,
                               messages: MessagesApi,
                               val eventHandler: EventHandler
                             ) extends SlackService {

  implicit val system = ActorSystem("slack")
  implicit val ec = system.dispatcher

  val clients = scala.collection.mutable.Map.empty[SlackBotProfile, SlackRtmClient]

  start()

  appLifecycle.addStopHook { () =>
    Future.successful(stop())
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
        val p = Promise[Unit]()
        val event = SlackMessageEvent(SlackMessageContext(client, profile, message))
        val handleMessage = for {
          maybeConversation <- event.context.maybeOngoingConversation(dataService)
          _ <- eventHandler.handle(event, maybeConversation).map { results =>
            results.foreach(_.sendIn(event.context, None, maybeConversation))
          }
        } yield {}
        p.completeWith(handleMessage)
        Future {
          Thread.sleep(500)
          while (!p.isCompleted) {
            client.indicateTyping(message.channel)
            Thread.sleep(3000)
          }
        }
      }
    }

  }

  def start(): Future[Any] = {
    stop()
    dataService.slackBotProfiles.allProfiles.map { profiles =>
      profiles.foreach { profile =>
        startFor(profile)
      }
    }
  }

  def stop() = {
    clients.clone().foreach { case(profile, client) =>
      client.close()
      clients.remove(profile)
    }
  }

}
