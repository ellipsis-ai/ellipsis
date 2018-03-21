package models.billing.invoice


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

  def allPendingInDescOrder(count: Int = 100): Future[Seq[Invoice]] = {
    Future {
      blocking {
        Invoice.list()
          .status().is(Status.PENDING)
          .sortByDate(SortOrder.DESC)
          .limit(count)
          .request(chargebeeEnv)
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
    allPendingInDescOrder().flatMap { invoices =>
      Future.sequence(invoices.map(toFatInvoice(_)))
    }
  }

  def addChargesForActiveUser(fatInvoice: FatInvoice, activeCount: Int): Future[FatInvoice] = {
    Future {
      blocking {
        Invoice.addAddonCharge(fatInvoice.invoice.id)
          .addonId(addonIdFor(fatInvoice.plan))
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
      maybePreviousInvoice <- previousInvoiceFor(fatInvoice)
      startDate <- Future.successful {
        maybePreviousInvoice match {
          case None => fatInvoice.subscription.createdAt()
          case Some(fi) => {
            fi.invoice.date()
          }
        }
      }
      endDate <- Future.successful(fatInvoice.invoice.date())
    } yield {
      BillingPeriod(startDate, endDate)
    }
  }

  private def toFatInvoice(invoice: Invoice): Future[FatInvoice] = {
    for {
      maybeSub <- dataService.subscriptions.find(invoice.subscriptionId())
      sub <- Future.successful {
        maybeSub.getOrElse(throw new InvalidInvoice("Subscription Id is invalid"))
      }
      maybePlan <- dataService.plans.find(sub.planId())
      plan <- Future.successful {
        maybePlan.getOrElse(throw new InvalidInvoice("Plan Id is invalid"))
      }
    } yield {
      FatInvoice(invoice, sub, plan)
    }
  }

  private def previousInvoiceFor(fatInvoice: FatInvoice): Future[Option[FatInvoice]] = {
    Future {
      blocking {
        val result: ListResult = Invoice.list()
          .subscriptionId().is(fatInvoice.subscription.id())
          .date().before(fatInvoice.invoice.date())
          .limit(1)
          .sortByDate(SortOrder.DESC)
          .request(chargebeeEnv)
        result
      }
    }.map { r =>
      if (r.length == 0) {
        None
      } else {
        Some(FatInvoice(r.get(0).invoice(), fatInvoice.subscription, fatInvoice.plan))
      }
    }
  }

}
