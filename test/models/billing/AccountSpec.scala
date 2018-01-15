package models.billing

import java.time.OffsetDateTime

import models.IDs
import models.billing.account.Account
import support.DBSpec

class AccountSpec extends DBSpec {

    "Billing Account" should {

      "be valid with valid params" in {
        withEmptyDB(dataService, { () =>
          val account = Account(IDs.next, IDs.next, OffsetDateTime.now)
          dataService.billingAccount.save(account)
        })
      }
    }

}
