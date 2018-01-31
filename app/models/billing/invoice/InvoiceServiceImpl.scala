package models.billing.invoice


import javax.inject.Inject

import com.chargebee.models.Invoice
import com.chargebee.models.Invoice.Status
import play.api.Configuration
import services.DataService

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future, blocking}


class InvoiceServiceImpl @Inject()(
                                 val configuration: Configuration,
                                 val dataService: DataService,
                                 implicit val ec: ExecutionContext
                               ) extends InvoiceService {

  def allPending(count: Int = 100): Future[Seq[Invoice]] = {
    Future {
      blocking {
        Invoice.list().status().is(Status.PENDING).limit(count).request(chargebeeEnv)
      }
    }.map { result =>
      val buffer = ListBuffer[com.chargebee.models.Invoice]()
      for (entry <- result) {
        buffer += entry.invoice()
      }
      buffer
    }
  }

  def addCharge(invoice: Invoice, chargeInCents: Int, description: String): Future[Invoice] = {
    Future {
      blocking {
        Invoice
          .addCharge(invoice.id).amount(chargeInCents)
          .description(description)
          .request()
      }
    }.map(_.invoice())
  }

  def close(invoice: Invoice): Future[Invoice] ={
    Future {
      blocking {
        Invoice.close(invoice.id).request()
      }
    }.map(_.invoice())
  }

}
