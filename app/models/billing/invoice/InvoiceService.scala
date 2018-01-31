package models.billing.invoice

import com.chargebee.models.Invoice
import models.billing.ChargebeeService

import scala.concurrent.Future


trait InvoiceService extends ChargebeeService {

  def allPending(count: Int = 100): Future[Seq[Invoice]]

  def addCharge(invoice: Invoice, chargeInCents: Int, description: String): Future[Invoice]

  def close(invoice: Invoice): Future[Invoice]

}
