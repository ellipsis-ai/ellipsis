package models.billing.subscription


import scala.concurrent.Future

trait SubscriptionService {

  def allAccounts: Future[Seq[Subscription]]

  def count: Future[Int]

  def find(id: String): Future[Option[Subscription]]

  //    def findChargebeeId(chargeBeeId: String): Future[Option[Account]]
  //
  //    def create(chargeBeeId: String): Future[Account]
  //
  //    def save(account: Account): Future[Account]
  //
  //    def delete(account: Account): Future[Account]
}
