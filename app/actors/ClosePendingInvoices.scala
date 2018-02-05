package actors

import javax.inject.Inject

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import play.api.Logger
import akka.actor.Actor
import services.billing.BillingService
import play.api.Configuration

object ClosePendingInvoices {
  final val name = "close-pending-invoices"
}

class ClosePendingInvoices @Inject() (
                                       val billingService: BillingService,
                                       val configuration: Configuration,
                                       implicit val ec: ExecutionContext
                                     ) extends Actor {

  // initial delay of 1 minute so that, in the case of errors & actor restarts, it doesn't hammer external APIs
  val tick = context.system.scheduler.schedule(1 minute, 30 minutes, self, "tick")
  val closePendingFlag = configuration.get[Boolean]("billing.process_pending_invoices")

  override def postStop() = {
    tick.cancel()
  }

  def receive = {
    case "tick" => {
      if (closePendingFlag) {
        billingService.processInvoices()
      } else {
        Logger.info("Billing message: Auto close pending info is off.")
      }
    }
  }

 }
