package models.billing.invoice


import java.time.{Instant, OffsetDateTime, ZoneId}
import javax.inject.Inject

import com.chargebee.ListResult
import com.chargebee.filters.enums.SortOrder
import com.chargebee.models.Invoice
import com.chargebee.models.Invoice.Status
import com.google.inject.Provider
import play.api.Configuration
import services.DataService

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future, blocking}


class InvoiceServiceImpl @Inject()(
                                 val configuration: Configuration,
                                 val dataServiceProvider: Provider[DataService],
                                 implicit val ec: ExecutionContext
                               ) extends InvoiceService {

  def dataService = dataServiceProvider.get

  def allPending(count: Int = 100): Future[Seq[Invoice]] = {
    Future {
      blocking {
        Invoice.list().status().is(Status.PENDING).limit(count).request(chargebeeEnv)
      }
    }.map { result =>
      val buffer = ListBuffer[com.chargebee.models.Invoice]()
      for (entry <- result) {
        buffer += entry.invoice
      }
      buffer
    }
  }

  def allPendingFatInvoices(): Future[Seq[FatInvoice]] = {
    allPending().flatMap { invoices =>
      Future.sequence(invoices.map(toFatInvoice(_)))
    }
  }

  def addChargesForActiveUser(fatInvoice: FatInvoice, activeCount: Int): Future[FatInvoice] = {
    Future {
      blocking {
        Invoice.addAddonCharge(fatInvoice.invoice.id)
          .addonId(addOnIdFor(fatInvoice.plan))
          .addonQuantity(activeCount)
          .request()
      }
    }.map { ni =>
      (FatInvoice(ni.invoice(), fatInvoice.subscription, fatInvoice.plan))
    }
  }

  def close(invoice: Invoice): Future[Invoice] ={
    Future {
      blocking {
        Invoice.close(invoice.id).request()
      }
    }.map(_.invoice())
  }

  def billingPeriodFor(fatInvoice: FatInvoice): Future[BillingPeriod] = {
    for {
      previousInvoice <- previousInvoiceFor(fatInvoice)
      startDate <- Future.successful(getInvoiceDateAsOffsetDateTimeFor(previousInvoice.invoice))
      endDate <- Future.successful(getInvoiceDateAsOffsetDateTimeFor(fatInvoice.invoice))
    } yield {
      BillingPeriod(startDate, endDate)
    }
  }

  private def toFatInvoice(invoice: Invoice): Future[FatInvoice] = {
    for {
      maybeSub <- dataService.subscriptions.get(invoice.subscriptionId())
      sub <- Future.successful {
        maybeSub.getOrElse(throw new InvalidInvoice("Subscription Id is invalid"))
      }
      maybePlan <- dataService.plans.get(sub.planId())
      plan <- Future.successful {
        maybePlan.getOrElse(throw new InvalidInvoice("Plan Id is invalid"))
      }
    } yield {
      FatInvoice(invoice, sub, plan)
    }
  }

  private def previousInvoiceFor(fatInvoice: FatInvoice): Future[FatInvoice] = {
    Future {
      blocking {
        val result: ListResult = Invoice.list().subscriptionId().is(fatInvoice.subscription.id())
          .status().isNot(Status.PAID)
          .limit(1)
          .sortByDate(SortOrder.DESC)
          .request(chargebeeEnv)
        result.get(0).invoice()
      }
    }.map(FatInvoice(_, fatInvoice.subscription, fatInvoice.plan))
  }

  private def getInvoiceDateAsOffsetDateTimeFor(invoice: Invoice): OffsetDateTime = {
    OffsetDateTime.ofInstant(Instant.ofEpochMilli(invoice.date().getTime), ZoneId.systemDefault())
  }

}
