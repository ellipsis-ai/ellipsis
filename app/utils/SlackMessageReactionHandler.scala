package utils

import akka.actor.ActorSystem
import play.api.Logger
import slack.api.SlackApiClient

import scala.concurrent.{ExecutionContext, Future, Promise}

object SlackMessageReactionHandler {

  val HOURGLASS_EMOJI_DURATION_MS = 3000 // ms

  def handle[T](client: SlackApiClient, future: Future[T], channel: String, messageTs: String, delayMilliseconds: Int = 1500)
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
      Thread.sleep(delayMilliseconds)
      if (!p.isCompleted) {
        client.addReactionToMessage("thinking_face", channel, messageTs).map(_ => {
          updateReactionClock(p, client, channel, messageTs)
        })
        p.future.onComplete(_ => {
          client.removeReactionFromMessage("thinking_face", channel, messageTs)
          client.removeReactionFromMessage("hourglass", channel, messageTs)
          client.removeReactionFromMessage("hourglass_flowing_sand", channel, messageTs)
        })
      }
    }
  }

  private def emojiFor(elapsedSeconds: Int): String = {
    if (elapsedSeconds % 2 == 1) { "hourglass" } else { "hourglass_flowing_sand" }
  }

  private def updateReactionClock[T](p: Promise[T], client: SlackApiClient, channel: String, messageTs: String, elapsedSeconds: Int = 0)
                            (implicit system: ActorSystem): Unit = {
    implicit val ec: ExecutionContext = system.dispatcher
    if (!p.isCompleted) {
      if (elapsedSeconds > 0) {
        client.removeReactionFromMessage(emojiFor(elapsedSeconds), channel, messageTs)
      }
      val next = elapsedSeconds + 1
      client.addReactionToMessage(emojiFor(next), channel, messageTs)
      Thread.sleep(HOURGLASS_EMOJI_DURATION_MS)
      updateReactionClock(p, client, channel, messageTs, next)
    }
  }
}
