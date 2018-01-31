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
import com.chargebee.models.Invoice
import services.billing.BillingService

object ClosePendingInvoices {
  final val name = "close-pending-invoices"
}

class ClosePendingInvoices @Inject() (
                                       dataService: DataService,
                                       billingService: BillingService,
                                       implicit val ec: ExecutionContext
                                     ) extends Actor {

  // initial delay of 1 minute so that, in the case of errors & actor restarts, it doesn't hammer external APIs
  val tick = context.system.scheduler.schedule(1 minute, 30 minutes, self, "tick")

  override def postStop() = {
    tick.cancel()
  }

  def receive = {
    case "tick" => {
      for {
        pendingInvoices <- dataService.invoices.allPending().map { invoices =>
          if (invoices.length > 0) {
            Logger.info(s"Found ${invoices.length} pending invoices.")
          }
          invoices
        }
        closedInvoices <- processPendingInvoices(pendingInvoices)
      } yield {}
    }
  }

  private def processPendingInvoices(invoices: Seq[Invoice]): Future[Seq[Invoice]] = {
    Future.sequence {
      invoices.map(billingService.addChargesAndClosePending(_))
    }
  }

}
