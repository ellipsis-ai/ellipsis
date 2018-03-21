package services.billing


import com.chargebee.models.Invoice

import scala.concurrent.Future


trait BillingService {

  def processInvoices: Future[Seq[Invoice]]

  def isActive: Boolean
}
