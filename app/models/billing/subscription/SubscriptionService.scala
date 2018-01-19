package models.billing.subscription

import scala.concurrent.Future

trait SubscriptionService {

  def allAccounts: Future[Seq[Subscription]]

  def count: Future[Int]

  def find(id: String): Future[Option[Subscription]]

}
