package utils

import akka.actor.ActorSystem
import play.api.Logger
import slack.api.SlackApiClient

import scala.concurrent.{ExecutionContext, Future, Promise}

object SlackMessageReactionHandler {

  val PROGRESS_EMOJI_DURATION_MS = 3000 // ms
  val INITIAL_REACTION = "thinking_face"
  val PROGRESS_REACTIONS = Seq("hourglass", "hourglass_flowing_sand")

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
        client.addReactionToMessage(INITIAL_REACTION, channel, messageTs).map(_ => {
          updateReactionClock(p, client, channel, messageTs)
        })
        p.future.onComplete(_ => {
          client.removeReactionFromMessage(INITIAL_REACTION, channel, messageTs)
          PROGRESS_REACTIONS.foreach(ea => client.removeReactionFromMessage(ea, channel, messageTs))
        })
      }
    }
  }

  private def emojiFor(elapsedSeconds: Int): String = {
    PROGRESS_REACTIONS(elapsedSeconds % PROGRESS_REACTIONS.length)
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
      Thread.sleep(PROGRESS_EMOJI_DURATION_MS)
      updateReactionClock(p, client, channel, messageTs, next)
    }
  }
}
