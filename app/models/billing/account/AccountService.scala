package models.billing.account

import scala.concurrent.Future

trait AccountService {

    def allAccounts: Future[Seq[Account]]

    def count: Future[Int]

    def find(id: String): Future[Option[Account]]

//    def findChargebeeId(chargeBeeId: String): Future[Option[Account]]
//
//    def create(chargeBeeId: String): Future[Account]
//
//    def save(account: Account): Future[Account]
//
//    def delete(account: Account): Future[Account]
}
