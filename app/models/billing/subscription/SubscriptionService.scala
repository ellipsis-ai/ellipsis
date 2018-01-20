package models.billing.subscription

import com.chargebee.models.Subscription
import com.chargebee.models.Plan
import models.billing.ChargebeeService

import scala.concurrent.Future


trait SubscriptionService extends ChargebeeService {

  def createFreeSubscription(teamId: String, organizationId: String, customerId: String): Future[Subscription]

}
