package actors

import javax.inject.Inject

import akka.actor.Actor
import models.bots.ScheduledMessageQueries
import services.{DataService, SlackService}
import slick.driver.PostgresDriver.api._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object ScheduledMessageActor {
  final val name = "scheduled-messages"
}

class ScheduledMessageActor @Inject() (val dataService: DataService, val slackService: SlackService) extends Actor {

  // initial delay of 1 minute so that, in the case of errors & actor restarts, it doesn't hammer external APIs
  val tick = context.system.scheduler.schedule(1 minute, 1 minute, self, "tick")

  override def postStop() = {
    tick.cancel()
  }

  def receive = {
    case "tick" => {
      val action = ScheduledMessageQueries.allToBeSent.flatMap { messages =>
        DBIO.sequence(messages.map { message =>
          message.botProfile(dataService).flatMap { maybeProfile =>
            maybeProfile.flatMap { profile =>
              slackService.clients.get(profile).map { client =>
                message.send(slackService, client, profile)
              }
            }.getOrElse(DBIO.successful(Unit))
          }
        })
      }.map { _ => true }

      dataService.runNow(action)
    }
  }
}
