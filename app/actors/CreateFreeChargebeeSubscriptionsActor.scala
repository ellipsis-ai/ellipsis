package actors

import javax.inject.Inject

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import play.api.Logger
import play.api.Configuration
import akka.actor.Actor
import models.organization.Organization
import services.DataService
import com.chargebee.models.Subscription
import models.IDs


object CreateFreeChargebeeSubscriptionsActor {
  final val name = "setup-chargebee-for-orgs"
}

class CreateFreeChargebeeSubscriptionsActor @Inject() (
                                             dataService: DataService,
                                             configuration: Configuration,
                                             implicit val ec: ExecutionContext
                                   ) extends Actor {

  val tick = context.system.scheduler.schedule(1 minute, 10 minutes, self, "tick")
  val createSubFlag = configuration.get[Boolean]("billing.auto_create_free_subscription")

  override def postStop() = {
    tick.cancel()
  }

  def receive = {
    case "tick" => {
      if (createSubFlag) {
        for {
          orgs <- dataService.organizations.allOrgsWithEmptyChargebeeId.map { orgs =>
            if (orgs.length > 0) {
              Logger.info(s"Found ${orgs.length} organizations without a Chargebee customer id.")
            }
            orgs
          }
          subs <- createSubsFor(orgs)
        } yield {}
      } else {
        Logger.info("Billing message: Creating free Chargebee subscriptions for new teams is OFF.")
      }
    }
  }

  private def createSubsFor(organizations: Seq[Organization]): Future[Seq[Option[Subscription]]] = {
    Future.sequence {
      organizations.map { org =>
        for {
          org <- dataService.organizations.setChargebeeCustomerIdFor(org, Some(IDs.next))
          sub <- createSub(org)
        } yield {
          sub
        }
      }
    }
  }

  private def createSub(org: Organization): Future[Option[Subscription]] = {
    dataService.subscriptions.createFreeSubscription(org).map { maybeSubscription =>
      Logger.info(s"Created Chargebee free subscription for Org ${org.name}, " +
        s"organization id: ${org.id}, Chargebee customer id: ${org.maybeChargebeeCustomerId}")
      maybeSubscription
    }
  }

}
