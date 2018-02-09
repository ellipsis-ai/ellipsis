package integrations.models.billing.invoice

import models.IDs
import support.BillingSpec

import scala.concurrent.Future


class InvoiceServiceSpec extends BillingSpec {


  "InvoiceServiceSpec.billingPeriodFor" should {

    "return a BillingPeriod object " in {
      withEmptyDB(dataService, { () =>
        val org = runNow(dataService.organizations.create(name = "myOrg", chargebeeCustomerId = IDs.next))
        val team = runNow(dataService.teams.create("myTeam", org))
        val user = newSavedUserOn(team)
        runNow(restChargebeeSite)
        val a = for {
          sub <- dataService.subscriptions.createFreeSubscription(org)
//          billingPeriod <- dataService.invoices.billingPeriodFor()
        } yield {}
        runNow(a)
      })
    }

  }

}

