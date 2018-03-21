package models.billing.addon

import com.chargebee.models.Addon

case class AddonData(
                      id: String,
                      name: String,
                      invoiceName: String,
                      price: Int,
                      unit: String = "active user",
                      currencyCode: String = "USD",
                      addType: Addon.Type = Addon.Type.QUANTITY,
                      chargeType: Addon.ChargeType = Addon.ChargeType.NON_RECURRING
                    )
object StandardAddons {

  val list = Seq(
    AddonData(
      id = "active-user-starter-v1" ,
      name = "Active User Starter(v1)",
      invoiceName = "Active Users",
      price = 500
    ),
    AddonData(
      id = "active-user-business-v1" ,
      name = "Active User Business(v1)",
      invoiceName = "Active Users",
      price = 800
    )
  )
}

