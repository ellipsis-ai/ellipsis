package actors

import javax.inject.Inject

import akka.actor.{Actor, ActorSystem}
import models.behaviors.events.EventHandler
import models.behaviors.scheduling.Scheduled
import play.api.{Configuration, Logger}
import services.DataService
import slack.api.SlackApiClient

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ScheduledActor {
  final val name = "scheduled"
}

class ScheduledActor @Inject()(
                                        val dataService: DataService,
                                        val eventHandler: EventHandler,
                                        val configuration: Configuration,
                                        implicit val actorSystem: ActorSystem
                                      ) extends Actor {

  // initial delay of 1 minute so that, in the case of errors & actor restarts, it doesn't hammer external APIs
  val tick = context.system.scheduler.schedule(1 minute, 1 minute, self, "tick")

  override def postStop() = {
    tick.cancel()
  }

  def receive = {
    case "tick" => {
      Scheduled.allToBeSent(dataService).flatMap { scheduleds =>
        Future.sequence(scheduleds.map { scheduled =>
          scheduled.displayText(dataService).flatMap { displayText =>
            scheduled.botProfile(dataService).flatMap { maybeProfile =>
              maybeProfile.map { profile =>
                scheduled.updateNextTriggeredFor(dataService).flatMap { _ =>
                  scheduled.send(eventHandler, new SlackApiClient(profile.token), profile, dataService, configuration)
                }
              }.getOrElse(Future.successful(Unit))
            }.recover {
              case t: Throwable => {
                Logger.error(s"Exception handling scheduled message: $displayText", t)
              }
            }
          }
        })
      }.
        map { _ => true }.
        recover {
          case t: Throwable => {
            Logger.error("Exception handling scheduled messages", t)
          }
        }
    }
  }
}
