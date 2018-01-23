package actors

import javax.inject.Inject
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import play.api.Logger
import akka.actor.Actor
import models.organization.Organization
import models.team.Team
import services.DataService
import com.chargebee.models.Subscription

object SetupChargebeeForOrgsActor {
  final val name = "setup-chargebee-for-orgs"
}

class SetupChargeebeForOrgsActor @Inject() (
                                             dataService: DataService,
                                             implicit val ec: ExecutionContext
                                   ) extends Actor {

  // initial delay of 1 minute so that, in the case of errors & actor restarts, it doesn't hammer external APIs
  val tick = context.system.scheduler.schedule(1 minute, 1 hour, self, "tick")

  override def postStop() = {
    tick.cancel()
  }

  def receive = {
    case "tick" => {
      for {
        orgs <- dataService.organizations.allOrgsWithEmptyChargebeeId
        subs <- createSubsFor(orgs)
      } yield {}
    }
  }

  private def createSubsFor(organizations: Seq[Organization]): Future[Seq[Option[Subscription]]] = {
    // Future[Seq[Seq[Option[Subscription]]]]
    Future.sequence {
      // Seq[Future[Seq[Option[Subscription]]]]
      organizations.map { org =>
        for {
          teams <- dataService.teams.allTeamsFor(org)
          subs <- teamsToSubs(teams, org)
        } yield {
          subs
        }
      }
    }.map(_.flatten)
  }

  private def teamsToSubs(teams: Seq[Team], org: Organization): Future[Seq[Option[Subscription]]] = {
    Future.sequence(teams.map(createSub(_, org)))
  }

  private def createSub(team: Team, org: Organization): Future[Option[Subscription]] = {
    dataService.subscriptions.createFreeSubscription(team, org)
      .recover {
        case e: Throwable => {
          Logger.error(s"Error while creating a free subscription for team ${team.name}", e)
          None
      }
    }
  }

}
