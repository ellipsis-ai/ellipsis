package integrations.models.billing.invoice

import com.chargebee.models.{Subscription, TimeMachine}
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

        // >>>>  MOVE BACK 6 MONTHS <<<<

        val timeMachine: TimeMachine = runNowAndBePatient(restChargebeeSite)
        val sub = runNow(dataService.subscriptions.createFreeSubscription(org))


        // >>>>  NOW MOVE 35 DAYS FORWARD <<<<

        val tm_35 = runNowAndBePatient(moveForward(timeMachine, 35))

        // There should be only one invoice in a pending state ...
        val pendingInvoices = runNow(dataService.invoices.allPendingFatInvoices())
        pendingInvoices.length mustBe 1
        val fatInvoice = pendingInvoices.head

        // with a billing period from the start of the subscription to the billing date ...
        val billingPeriod = runNow(dataService.invoices.billingPeriodFor(fatInvoice))

        billingPeriod.start mustBe fatInvoice.subscription.startedAt()
        billingPeriod.end mustBe fatInvoice.invoice.date()

        // and the sub should still be active.
        fatInvoice.subscription.status() mustBe Subscription.Status.ACTIVE

        // >>>>  NOW MOVE FORWARD 30 DAYS MORE <<<<
        val tm_65 = runNowAndBePatient(moveForward(tm_35, 30))

      })
    }

  }

}

