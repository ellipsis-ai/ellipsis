package services.billing

import java.time.{Instant, OffsetDateTime, ZoneId}
import javax.inject.Inject

import com.chargebee.models.Invoice
import models.billing.invoice.FatInvoice
import play.api.Configuration
import services.DataService
import services.stats.StatsService

import scala.concurrent.{ExecutionContext, Future}

class BillingServiceImpl @Inject()(
                                    val configuration: Configuration,
                                    val dataService: DataService,
                                    val statsService: StatsService,
                                    implicit val ec: ExecutionContext
                                  ) extends BillingService {

  case class InvalidBillingRecord(message: String) extends Exception(message)


  def processInvoices(): Future[Seq[Invoice]] = {
    for {

      // Fetch all the pending invoices and make them Fat (get the subscription and the plan) so that
      // we can run some biz logic on the invoices
      pending <- dataService.invoices.allPendingFatInvoices()

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

  private def pendingAndMetered(pendingFatInvoice: Seq[FatInvoice]): Future[Seq[FatInvoice]] = {
    Future {
      pendingFatInvoice.filter(fi => fi.plan.optBoolean("cf_metered") == true)
    }
  }

  private def chargeMetered(pendingAndMetered: Seq[FatInvoice]): Future[Seq[FatInvoice]] = {
    Future.sequence {
      pendingAndMetered.map { fi =>
        for {
          activeCount <- getActiveUserCountFor(fi)
          chargedInvoice <- dataService.invoices.addChargesForActiveUser(fi, activeCount)
        } yield {
          chargedInvoice
        }
      }
    }
  }

  private def getActiveUserCountFor(meteredFatInvoice: FatInvoice): Future[Int] = {
    for {
      billingPeriod <- dataService.invoices.billingPeriodFor(meteredFatInvoice)
      orgId <- Future.successful(meteredFatInvoice.subscription.optString("cf_organization_id"))
      org <- dataService.organizations.find(orgId)
      count <- statsService.activeUsersCountFor(org.get, billingPeriod.start, billingPeriod.end)
    } yield {
       count
    }
  }

  private def closeAll(pending: Seq[FatInvoice]): Future[Seq[FatInvoice]] = {
    Future.sequence{
      pending.map { fatInvoice =>
        dataService.invoices.close(fatInvoice.invoice).map(_=>fatInvoice)
      }
    }
  }

}
