package services.billing

import java.time.{Instant, OffsetDateTime, ZoneId}
import javax.inject.Inject

import com.amazonaws.services.inspector.model.Subscription
import com.chargebee.models.{Invoice, Plan}
import com.chargebee.models.Invoice.Status
import models.billing.invoice.FatInvoice
import models.organization.Organization
import play.api.{Configuration, Logger}
import services.DataService

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future, blocking}


case class Charge(amountInCents: Int, description: String)

class BillingServiceImpl @Inject()(
                                    val configuration: Configuration,
                                    val dataService: DataService,
                                    implicit val ec: ExecutionContext
                                  ) extends BillingService {

  case class InvalidBillingRecord(message: String) extends Exception(message)


  def processInvoices(): Future[Seq[Invoice]] = {
    for {

      // Fetch all the pending invoices and make them Fat (get the subscription and the plan) so that
      // we can run some biz logic on the invoices
      pending <- pendingFatInvoices()

      // Find out the list of invoices due to a metered subscription
      pendingAndMetered <- pendingAndMetered(pending)

      // Charge the metered invoices
      pendingAndMeteredAndCharged <- chargeMetered(pendingAndMetered)

      // Close all the pending invoices
      closed <- closeAll(pending)
    } yield {
      closed.map(_.invoice)
    }
  }

  private def pendingFatInvoices(): Future[Seq[FatInvoice]] = {
    dataService.invoices.allPending().flatMap { invoices =>
      Future.sequence(invoices.map(dataService.invoices.toFatInvoice(_)))
    }
  }

  private def pendingAndMetered(pendingFatInvoice: Seq[FatInvoice]): Future[Seq[FatInvoice]] = {
    Future {
      pendingFatInvoice.filter(fi => fi.plan.optBoolean("cf_metered") == true)
    }
  }

  private def chargeMetered(pendingAndMetered: Seq[FatInvoice]): Future[Seq[FatInvoice]] = {
    Future { pendingAndMetered }
//    pendingAndMetered.map { fi =>
//      val bp = dataService.invoices.billingPeriodFor(fi)
//      val orgId = fi.subscription.organizationId
//      val activeUsers = dataService.teamStats.activeUsersCount(bp.start, bp.end)
//    }

  }

  private def closeAll(pending: Seq[FatInvoice]): Future[Seq[FatInvoice]] = {
    Future.sequence{
      pending.map { fatInvoice =>
        dataService.invoices.close(fatInvoice.invoice).map(_=>fatInvoice)
      }
    }
  }

}
