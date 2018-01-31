package services.billing

import javax.inject.Inject

import com.chargebee.models.Invoice
import com.chargebee.models.Invoice.Status
import play.api.Configuration
import services.DataService

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future, blocking}


class BillingServiceImpl @Inject()(
                                    val configuration: Configuration,
                                    val dataService: DataService,
                                    implicit val ec: ExecutionContext
                                  ) extends BillingService {

  def addChargesAndClosePending(invoice: Invoice): Future[Invoice] = {
//    get the number of active users for the dates
//    add the charge
    dataService.invoices.close(invoice)
  }

}
