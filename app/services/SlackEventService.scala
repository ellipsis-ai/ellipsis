package services

import javax.inject._

import akka.actor.ActorSystem
import play.api.i18n.MessagesApi
import models.behaviors.events.{EventHandler, SlackMessageEvent}
import play.api.Logger

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Random

@Singleton
class SlackEventService @Inject()(
                                   val dataService: DataService,
                                   messages: MessagesApi,
                                   val eventHandler: EventHandler
                                 ) {

  implicit val system = ActorSystem("slack")
  implicit val ec: ExecutionContext = system.dispatcher

  val random = new Random()

  def onEvent(event: SlackMessageEvent): Future[Unit] = {
    if (!event.isBotMessage) {
      val p = Promise[Unit]()
      val handleMessage = for {
        maybeConversation <- event.maybeOngoingConversation(dataService)
        _ <- eventHandler.handle(event, maybeConversation).flatMap { results =>
          maybeConversation.map(c => Future.successful(Some(c))).getOrElse(event.maybeConversationRootedHere(dataService)).flatMap { maybeConversation =>
            Future.sequence(
              results.map(result => result.sendIn(None, maybeConversation).map { _ =>
                Logger.info(s"Sending result [${result.fullText}] in response to slack message [${event.fullMessageText}] in channel [${event.channel}]")
              })
            )
          }
        }
      } yield {}
      handleMessage.recover {
        case t: Throwable => {
          Logger.error("Exception responding to a Slack message", t)
        }
      }
      p.completeWith(handleMessage)
      Future {
        Thread.sleep(1500)
        if (!p.isCompleted) {
          event.client.addReactionToMessage("thinking_face", event.channel, event.ts).map { _ =>
            p.future.onComplete(_ => event.client.removeReactionFromMessage("thinking_face", event.channel, event.ts))
          }
        }
      }
    } else {
      Future.successful({})
    }
  }

}
