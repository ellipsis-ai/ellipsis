package actors

import javax.inject.Inject

import akka.actor.Actor
import models.bots.triggers.ScheduleTriggerQueries
import services.{SlackService, AWSLambdaService}
import slick.driver.PostgresDriver.api._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object ScheduleTriggerActor {
  final val name = "schedule-trigger"
}

class ScheduleTriggerActor @Inject() (val awsService: AWSLambdaService, val slackService: SlackService) extends Actor {

  val models = awsService.models

  val tick = context.system.scheduler.schedule(Duration.Zero, 1 minute, self, "tick")

  override def postStop() = {
    tick.cancel()
  }

  def receive = {
    case "tick" => {
      val action = ScheduleTriggerQueries.allToBeTriggered.flatMap { triggers =>
        DBIO.sequence(triggers.map { trigger =>
          trigger.botProfile.flatMap { maybeProfile =>
            maybeProfile.flatMap { profile =>
              slackService.clients.get(profile).map { client =>
                trigger.run(awsService, client)
              }
            }.getOrElse(DBIO.successful(Unit))
          }
        })
      }.map { _ => true }

      models.runNow(action)
    }
  }
}
