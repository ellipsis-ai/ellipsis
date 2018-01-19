package models.billing

import java.time.OffsetDateTime

import models.IDs
import models.billing.account.Customer
import support.DBSpec

class AccountSpec extends DBSpec {

    "Billing Account" should {

      "be valid with valid params" in {
        withEmptyDB(dataService, { () =>
          val account = Customer(IDs.next, IDs.next, OffsetDateTime.now)
          dataService.billingAccount.save(account)
        })
      }
    }

}
