package services

import javax.inject._

import play.api.i18n.MessagesApi
import akka.actor.ActorSystem
import models.behaviors.events.EventHandler
import play.api.Logger
import services.slack._

import scala.concurrent.{Future, Promise}

@Singleton
class SlackEventService @Inject()(
                                   val dataService: DataService,
                                   messages: MessagesApi,
                                   val eventHandler: EventHandler
                                 ) {

  implicit val system = ActorSystem("slack")
  implicit val ec = system.dispatcher

  def onEvent(event: NewSlackMessageEvent): Future[Unit] = {
    if (!event.isBotMessage) {
      val handleMessage = for {
        maybeConversation <- event.maybeOngoingConversation(dataService)
        _ <- eventHandler.handle(event, maybeConversation).flatMap { results =>
          Future.sequence(
            results.map(_.sendIn(event, None, maybeConversation))
          )
        }
      } yield {}
      handleMessage.recover {
        case t: Throwable => {
          Logger.error("Exception responding to a Slack message", t)
        }
      }
    } else {
      Future.successful({})
    }
  }

}
