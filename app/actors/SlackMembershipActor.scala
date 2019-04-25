package actors

import javax.inject.Inject

import akka.actor.Actor
import services.DataService

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object SlackMembershipActor {
  final val name = "slack-memberships"
}

class SlackMembershipActor @Inject() (
                                           dataService: DataService,
                                           implicit val ec: ExecutionContext
                                         ) extends Actor {

  // initial delay of 1 minute so that, in the case of errors & actor restarts, it doesn't hammer external APIs
  val tick = context.system.scheduler.schedule(1 minute, 1 day, self, "tick")

  override def postStop() = {
    tick.cancel()
  }

  def receive = {
    case "tick" => dataService.slackMemberStatuses.updateAll
  }
}
