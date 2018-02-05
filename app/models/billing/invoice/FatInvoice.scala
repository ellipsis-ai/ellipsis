package models.billing.invoice

import com.chargebee.models.{Invoice, Plan, Subscription}

case class FatInvoice(invoice: Invoice, subscription: Subscription, plan: Plan) {
  def organizationId: String = {
    subscription.optString("cf_organization_id")
  }
}
