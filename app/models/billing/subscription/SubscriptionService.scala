package models.billing.subscription


import scala.concurrent.Future
import com.chargebee.models.Subscription
import models.organization.Organization


trait SubscriptionService {

  def createFreeSubscription(organization: Organization): Future[Option[Subscription]]

  def find(subscriptionId: String): Future[Option[Subscription]]

  def allSubscriptions(count: Int = 100): Future[Seq[Subscription]]

  def delete(subscription: Subscription): Future[Option[Subscription]]

  def delete(subscriptions: Seq[Subscription]): Future[Seq[Option[Subscription]]]
}
