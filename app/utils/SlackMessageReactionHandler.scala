package utils

import akka.actor.ActorSystem
import play.api.Logger
import slack.api.SlackApiClient

import scala.concurrent.{ExecutionContext, Future, Promise}

object SlackMessageReactionHandler {
  def handle[T](client: SlackApiClient, future: Future[T], channel: String, messageTs: String)
               (implicit system: ActorSystem): Future[Unit] = {
    implicit val ec: ExecutionContext = system.dispatcher
    val p = Promise[T]()
    future.recover {
      case t: Throwable => {
        Logger.error("Exception responding to a Slack message", t)
      }
    }
    p.completeWith(future)
    Future {
      Thread.sleep(1500)
      if (!p.isCompleted) {
        client.addReactionToMessage("thinking_face", channel, messageTs).map { _ =>
          p.future.onComplete(_ => client.removeReactionFromMessage("thinking_face", channel, messageTs))
        }
      }
    }
  }
}
