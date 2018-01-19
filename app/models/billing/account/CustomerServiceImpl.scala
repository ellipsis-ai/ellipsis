package models.billing.account

import javax.inject.Inject

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

class CustomerServiceImpl @Inject()(
                                    dataServiceProvider: Provider[DataService],
                                    implicit val ec: ExecutionContext
                                  ) extends CustomerService {

  def dataService = dataServiceProvider.get

  import CustomerQueries._

  def allAccounts: Future[Seq[Customer]] = {
    dataService.run(all.result)
  }

  def count: Future[Int] = {
    dataService.run(all.length.result)
  }

  def find(id: String): Future[Option[Customer]] = {
    dataService.run(findQueryFor(id).result.map(_.headOption))
  }

//  def findChargebeeId(chargeBeeId: String): Future[Option[Account]] = {}
//
//  def create(chargeBeeId: String): Future[Account] = {}
//
//  def save(account: Account): Future[Account] = {}
//
//  def delete(account: Account): Future[Account] = {}

}
