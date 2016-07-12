package actors

import javax.inject.Inject

import akka.actor.Actor
import models.bots.triggers.ScheduleTriggerQueries
import services.AWSLambdaService
import slack.rtm.SlackRtmClient
import slick.driver.PostgresDriver.api._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object ScheduleTriggerActor {
  final val name = "schedule-trigger"
}

class ScheduleTriggerActor @Inject() (val service: AWSLambdaService, val client: SlackRtmClient) extends Actor {

  val models = service.models

  val tick = context.system.scheduler.schedule(Duration.Zero, 1 minute, self, "tick")

  override def postStop() = {
    tick.cancel()
  }

  def receive = {
    case "tick" => {
      val action = ScheduleTriggerQueries.allToBeTriggered.flatMap { triggers =>
        DBIO.sequence(triggers.map(_.run(service, client))).map { _ => true }
      }

      models.runNow(action)
    }
  }
}
