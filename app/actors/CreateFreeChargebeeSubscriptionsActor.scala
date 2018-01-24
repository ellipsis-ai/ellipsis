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
import models.IDs

object CreateFreeChargebeeSubscriptionsActor {
  final val name = "setup-chargebee-for-orgs"
}

class CreateFreeChargebeeSubscriptionsActor @Inject() (
                                             dataService: DataService,
                                             implicit val ec: ExecutionContext
                                   ) extends Actor {

  // initial delay of 1 minute so that, in the case of errors & actor restarts, it doesn't hammer external APIs
  val tick = context.system.scheduler.schedule(1 minute, 10 minutes, self, "tick")

  override def postStop() = {
    tick.cancel()
  }

  def receive = {
    case "tick" => {
      for {
        orgs <- dataService.organizations.allOrgsWithEmptyChargebeeId.map { orgs =>
          if (orgs.length > 0) {
            Logger.info(s"Found ${orgs.length} organizations without a Chargebee customer id.")
          }
          orgs
        }
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
          org <- dataService.organizations.setChargebeeCustomerIdFor(org, Some(IDs.next))
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
    dataService.subscriptions.createFreeSubscription(team, org).map { maybeSubscription =>
      Logger.info(s"Created Chargebee free subscription for team ${team.name}, " +
        s"organization id: ${org.id}, Chargebee customer id: ${org.maybeChargebeeCustomerId}")
      maybeSubscription
    }
  }

}
