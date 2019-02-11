package utils

import akka.actor.ActorSystem
import services.ms_teams.MSTeamsApiClient
import services.ms_teams.apiModels.{ActivityInfo, EventInfo}

import scala.concurrent.{ExecutionContext, Future, Promise}

object MSTeamsMessageReactionHandler {

  val PROGRESS_EMOJI_DURATION_MS = 2500
  val MAX_REACTION_COUNT = 8

  def handle[T](client: MSTeamsApiClient, future: Future[T], info: EventInfo, delayMilliseconds: Int = 1500)
               (implicit system: ActorSystem): Future[Unit] = {
    info match {
      case info: ActivityInfo => {
        implicit val ec: ExecutionContext = system.dispatcher
        val p = Promise[T]()
        p.completeWith(future)
        Future {
          Thread.sleep(delayMilliseconds)
          if (!p.isCompleted) {
            client.indicateTyping(info).map(_ => {
              if (!p.isCompleted) {
                updateReactionProgress(p, client, info)
              }
            })
            p.future
          }
        }
      }
      case _ => Future.successful({})
    }
  }

  private def updateReactionProgress[T](p: Promise[T], client: MSTeamsApiClient, info: ActivityInfo, updateCount: Int = 0)
                                       (implicit system: ActorSystem): Unit = {
    implicit val ec: ExecutionContext = system.dispatcher
    if (!p.isCompleted) {
      if (updateCount < MAX_REACTION_COUNT) {
        val next = updateCount + 1
        client.indicateTyping(info).map(_ => {
          if (!p.isCompleted) {
            Thread.sleep(PROGRESS_EMOJI_DURATION_MS)
            updateReactionProgress(p, client, info, next)
          }
        })
      }
    }
  }
}
