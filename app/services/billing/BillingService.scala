package services.billing

import com.chargebee.models.Invoice

import scala.concurrent.Future


trait BillingService {

  def addChargesAndClosePending(invoice: Invoice): Future[Invoice]

}
