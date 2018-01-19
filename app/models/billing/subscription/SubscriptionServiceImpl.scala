package models.billing.subscription


import javax.inject.Inject

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

class SubscriptionServiceImpl @Inject()(
                                    dataServiceProvider: Provider[DataService],
                                    implicit val ec: ExecutionContext
                                  ) extends SubscriptionService{

  def dataService = dataServiceProvider.get

  import SubscriptionQueries._

  def allAccounts: Future[Seq[Subscription]] = {
    dataService.run(all.result)
  }

  def count: Future[Int] = {
    dataService.run(all.length.result)
  }

  def find(id: String): Future[Option[Subscription]] = {
    dataService.run(findQueryFor(id).result.map(_.headOption))
  }


}
