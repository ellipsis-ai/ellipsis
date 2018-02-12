package integrations.models.billing.invoice

import com.chargebee.models.{Subscription, TimeMachine}
import integrations.support.BillingSpec
import models.IDs
import tags.{BillingTest, IntegrationTest}

@IntegrationTest @BillingTest
class InvoiceServiceSpec extends BillingSpec {

  "The Invoice Service" should  {

    "return the correct billing period for invoices" in {
      withEmptyDB(dataService, { () =>
        val org = runNow(dataService.organizations.create(name = "myOrg", chargebeeCustomerId = IDs.next))
        val team = runNow(dataService.teams.create("myTeam", org))
        val user = newSavedUserOn(team)

        // >>>>  MOVE BACK ~3 MONTHS <<<<
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
        val tm_65 = runNowAndBePatient(moveForward(timeMachine, 65))

        // Now there should be 2 invoices in pending state ...
        val pendingInvoices2 = runNow(dataService.invoices.allPendingFatInvoices())
        pendingInvoices2.length mustBe 2

        val lastFatInvoice = pendingInvoices2(0)
        val previousFatInvoice = pendingInvoices2(1)


        // with a billing period that starts date of the previous invoice ...
        val billingPeriod2 = runNow(dataService.invoices.billingPeriodFor(lastFatInvoice))
        billingPeriod2.start mustBe previousFatInvoice.invoice.date()
        billingPeriod2.end mustBe lastFatInvoice.invoice.date()

        // and the sub should still be active.
        lastFatInvoice.subscription.status() mustBe Subscription.Status.ACTIVE

      })
    }

  }

}

