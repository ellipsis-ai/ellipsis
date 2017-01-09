package services

import javax.inject._

import akka.actor.ActorSystem
import play.api.i18n.MessagesApi
import models.behaviors.events.EventHandler
import play.api.Logger
import services.slack._

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

  lazy val loadingMessages = Seq(
    ":thinking_face: …"
  )

  val random = new Random()

  def loadingMessage = {
    val index = random.nextInt(loadingMessages.length)
    loadingMessages(index)
  }

  def onEvent(event: SlackMessageEvent): Future[Unit] = {
    if (!event.isBotMessage) {
      val p = Promise[Unit]()
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
      p.completeWith(handleMessage)
      Future {
        Thread.sleep(1500)
        if (!p.isCompleted) {
          event.client.postChatMessage(
            event.channel,
            loadingMessage,
            asUser = Some(true),
            unfurlLinks = None,
            unfurlMedia = None
          ).map { ts =>
            p.future.onComplete(_ => event.client.deleteChat(event.channel, ts))
          }
        }
      }
    } else {
      Future.successful({})
    }
  }

}
