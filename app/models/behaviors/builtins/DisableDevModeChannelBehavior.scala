package models.behaviors.builtins

import akka.actor.ActorSystem
import models.behaviors.behaviorversion.Normal
import models.behaviors.events.Event
import models.behaviors.{BotResult, SimpleTextResult}
import services.DefaultServices

import scala.concurrent.{ExecutionContext, Future}


case class DisableDevModeChannelBehavior(
                                          event: Event,
                                          services: DefaultServices
                                        ) extends BuiltinBehavior {

  def result(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[BotResult] = {
    val dataService = services.dataService
    val problemResponse = SimpleTextResult(event, None, "There was a problem trying to disable dev mode", responseType = Normal)
    event.maybeChannel.map { channel =>
      for {
        maybeTeam <- dataService.teams.find(event.teamId)
        maybeDVC <- maybeTeam.map { team =>
          dataService.devModeChannels.find(event.context, channel, team)
        }.getOrElse(Future.successful(None))
        result <- maybeDVC.map { dvc =>
          dataService.devModeChannels.delete(dvc).map { _ =>
            SimpleTextResult(event, None, s"OK, this channel is back in normal mode", responseType = Normal)
          }
        }.getOrElse(Future.successful(SimpleTextResult(event, None, "This channel was already in normal mode, so I left it there", responseType = Normal)))
      } yield result
    }.getOrElse {
      Future.successful(problemResponse)
    }
  }

}
