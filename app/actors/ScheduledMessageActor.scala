package actors

import javax.inject.Inject

import akka.actor.Actor
import models.behaviors.events.EventHandler
import play.api.{Configuration, Logger}
import services.DataService
import slack.api.SlackApiClient

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ScheduledMessageActor {
  final val name = "scheduled-messages"
}

class ScheduledMessageActor @Inject() (
                                        val dataService: DataService,
                                        val eventHandler: EventHandler,
                                        val configuration: Configuration
                                      ) extends Actor {

  // initial delay of 1 minute so that, in the case of errors & actor restarts, it doesn't hammer external APIs
  val tick = context.system.scheduler.schedule(1 minute, 1 minute, self, "tick")

  override def postStop() = {
    tick.cancel()
  }

  def receive = {
    case "tick" => {
      dataService.scheduledMessages.allToBeSent.flatMap { messages =>
        Future.sequence(messages.map { message =>
          message.botProfile(dataService).flatMap { maybeProfile =>
            maybeProfile.map { profile =>
              dataService.scheduledMessages.updateNextTriggeredFor(message).flatMap { _ =>
                message.send(eventHandler, new SlackApiClient(profile.token), profile, dataService, configuration)
              }
            }.getOrElse(Future.successful(Unit))
          }.recover {
            case t: Throwable => {
              Logger.error(s"Exception handling scheduled message: ${message.text}", t)
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
