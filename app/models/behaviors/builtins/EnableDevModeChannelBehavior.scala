package models.behaviors.builtins

import akka.actor.ActorSystem
import models.behaviors.behaviorversion.Normal
import models.behaviors.events.Event
import models.behaviors.{BotResult, SimpleTextResult}
import services.DefaultServices

import scala.concurrent.{ExecutionContext, Future}


case class EnableDevModeChannelBehavior(
                                       event: Event,
                                       services: DefaultServices
                                     ) extends BuiltinBehavior {

  def result(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[BotResult] = {
    val dataService = services.dataService
    val problemResponse = SimpleTextResult(event, None, "There was a problem trying to enable dev mode", responseType = Normal)
    event.maybeChannel.map { channel =>
      for {
        maybeTeam <- dataService.teams.find(event.ellipsisTeamId)
        maybeExisting <- maybeTeam.map { team =>
          dataService.devModeChannels.find(event.eventContext.name, channel, team)
        }.getOrElse(Future.successful(None))
        result <- maybeTeam.map { team =>
          maybeExisting.map { _ =>
            Future.successful(SimpleTextResult(event, None, s"This channel was already in dev mode, so I left it there", responseType = Normal))
          }.getOrElse {
            dataService.devModeChannels.ensureFor(event.eventContext.name, channel, team).map { _ =>
              SimpleTextResult(event, None, s"OK, this channel is now in dev mode", responseType = Normal)
            }
          }
        }.getOrElse(Future.successful(problemResponse))
      } yield result
    }.getOrElse {
      Future.successful(problemResponse)
    }
  }

}
