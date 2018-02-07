package models.billing.invoice

import com.chargebee.models.Invoice
import models.billing.ChargebeeService

import scala.concurrent.Future


trait InvoiceService extends ChargebeeService {

  def allPending(count: Int = 100): Future[Seq[Invoice]]

  def addChargesForActiveUser(fatInvoice: FatInvoice, activeCount: Int): Future[FatInvoice]

  def close(invoice: Invoice): Future[Invoice]

  def billingPeriodFor(fatInvoice: FatInvoice): Future[BillingPeriod]

  def allPendingFatInvoices(): Future[Seq[FatInvoice]]

}
