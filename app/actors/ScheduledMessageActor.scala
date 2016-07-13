package actors

import javax.inject.Inject

import akka.actor.Actor
import models.Models
import models.bots.ScheduledMessageQueries
import services.SlackService
import slick.driver.PostgresDriver.api._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object ScheduledMessageActor {
  final val name = "scheduled-messages"
}

class ScheduledMessageActor @Inject() (val models: Models, val slackService: SlackService) extends Actor {

  val tick = context.system.scheduler.schedule(Duration.Zero, 1 minute, self, "tick")

  override def postStop() = {
    tick.cancel()
  }

  def receive = {
    case "tick" => {
      val action = ScheduledMessageQueries.allToBeSent.flatMap { messages =>
        DBIO.sequence(messages.map { message =>
          message.botProfile.flatMap { maybeProfile =>
            maybeProfile.flatMap { profile =>
              slackService.clients.get(profile).map { client =>
                message.send(slackService, client, profile)
              }
            }.getOrElse(DBIO.successful(Unit))
          }
        })
      }.map { _ => true }

      models.runNow(action)
    }
  }
}
