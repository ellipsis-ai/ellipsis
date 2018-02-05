package models.billing.invoice

import java.time.OffsetDateTime

import com.amazonaws.services.inspector.model.Subscription
import com.chargebee.models.Invoice
import models.billing.ChargebeeService
import services.billing.Charge

import scala.concurrent.Future


trait InvoiceService extends ChargebeeService {

  def allPending(count: Int = 100): Future[Seq[Invoice]]

  def addCharges(invoice: Invoice, charges: Seq[Charge]): Future[Invoice]

  def close(invoice: Invoice): Future[Invoice]

  def toFatInvoice(invoice: Invoice): Future[FatInvoice]

  def previousInvoiceFor(fatInvoice: FatInvoice): Future[FatInvoice]

  def billingPeriodFor(fatInvoice: FatInvoice): Future[BillingPeriod]


}
