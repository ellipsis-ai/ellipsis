package models.billing.account

import scala.concurrent.Future

trait CustomerService {

    def allAccounts: Future[Seq[Customer]]

    def count: Future[Int]

    def find(id: String): Future[Option[Customer]]

//    def findChargebeeId(chargeBeeId: String): Future[Option[Account]]
//
//    def create(chargeBeeId: String): Future[Account]
//
//    def save(account: Account): Future[Account]
//
//    def delete(account: Account): Future[Account]
}
