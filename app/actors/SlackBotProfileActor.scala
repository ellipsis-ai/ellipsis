package actors

import javax.inject.Inject

import akka.actor.Actor
import models.Models
import models.accounts.SlackBotProfileQueries
import org.joda.time.DateTime
import services.SlackService

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object SlackBotProfileActor {
  final val name = "slack-bot-profiles"
}

class SlackBotProfileActor @Inject() (val models: Models, val slackService: SlackService) extends Actor {

  val tick = context.system.scheduler.schedule(Duration.Zero, 10 seconds, self, "tick")

  var nextCutoff: DateTime = DateTime.now

  override def postStop() = {
    tick.cancel()
  }

  def receive = {
    case "tick" => {
      val cutoff = nextCutoff
      nextCutoff = DateTime.now.minusSeconds(1)
      val action = SlackBotProfileQueries.allSince(cutoff).map { profiles =>
        profiles.map { profile =>
          slackService.startFor(profile)
        }
      }.map { _ => true }

      models.runNow(action)
    }
  }
}
