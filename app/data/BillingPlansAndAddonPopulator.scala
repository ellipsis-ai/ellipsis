package data


import javax.inject._

import play.api.{Configuration, Logger}
import services.DataService
import services.billing.BillingService

import scala.concurrent.ExecutionContext

class BillingPlansAndAddonsPopulator @Inject() (
                                                 billingService: BillingService,
                                                 dataService: DataService,
                                                 implicit val ec: ExecutionContext
                                               ) {

  def run(): Unit = {
    if (billingService.isActive) {
      Logger.info("Billing system is ON.")
      Logger.info("Creating Plans and Addons as necessary.")
      val f = for {
        _ <- dataService.plans.createStandardPlans(doNotLogError=true)
        _ <- dataService.addons.createStandardAddons(doNotLogError=true)
      } yield {}
      dataService.runNow(f)
    } else {
      Logger.info("Billing system is OFF.")
    }
  }

  run()
}
