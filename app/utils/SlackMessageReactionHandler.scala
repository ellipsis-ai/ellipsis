package utils

import akka.actor.ActorSystem
import play.api.Logger
import slack.api.SlackApiClient

import scala.concurrent.{ExecutionContext, Future, Promise}

object SlackMessageReactionHandler {

  val PROGRESS_EMOJI_DURATION_MS = 2500
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
          updateReactionProgress(p, client, channel, messageTs)
        })
        p.future.onComplete(_ => removeAll(client, channel, messageTs))
      }
    }
  }

  private def removeAll(client: SlackApiClient, channel: String, messageTs: String)
                       (implicit system: ActorSystem): Unit = {
    implicit val ec: ExecutionContext = system.dispatcher
    client.removeReactionFromMessage(INITIAL_REACTION, channel, messageTs)
    PROGRESS_REACTIONS.foreach(ea => client.removeReactionFromMessage(ea, channel, messageTs))
  }

  private def emojiFor(updateCount: Int): String = {
    PROGRESS_REACTIONS(updateCount % PROGRESS_REACTIONS.length)
  }

  private def updateReactionProgress[T](p: Promise[T], client: SlackApiClient, channel: String, messageTs: String, updateCount: Int = 0)
                            (implicit system: ActorSystem): Unit = {
    implicit val ec: ExecutionContext = system.dispatcher
    if (!p.isCompleted) {
      if (updateCount > 0) {
        client.removeReactionFromMessage(emojiFor(updateCount), channel, messageTs)
      }
      val next = updateCount + 1
      val nextEmoji = emojiFor(next)
      client.addReactionToMessage(nextEmoji, channel, messageTs).map(_ => {
        if (p.isCompleted) {
          client.removeReactionFromMessage(nextEmoji, channel, messageTs)
        } else {
          Thread.sleep(PROGRESS_EMOJI_DURATION_MS)
          updateReactionProgress(p, client, channel, messageTs, next)
        }
      })
    }
  }
}
