package actors

import javax.inject.Inject

import akka.actor.Actor
import org.joda.time.LocalDateTime
import services.{DataService, SlackService}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object SlackBotProfileActor {
  final val name = "slack-bot-profiles"
}

class SlackBotProfileActor @Inject() (
                                       val dataService: DataService,
                                       val slackService: SlackService
                                     ) extends Actor {

  val tick = context.system.scheduler.schedule(Duration.Zero, 10 seconds, self, "tick")

  var nextCutoff: LocalDateTime = LocalDateTime.now

  override def postStop() = {
    tick.cancel()
  }

  def receive = {
    case "tick" => {
      val cutoff = nextCutoff
      nextCutoff = LocalDateTime.now.minusSeconds(1)
      val startFuture = dataService.slackBotProfiles.allSince(cutoff).map { profiles =>
        profiles.foreach { profile =>
          slackService.startFor(profile)
        }
      }.map { _ => true }
      dataService.runNow(startFuture)
    }
  }
}
