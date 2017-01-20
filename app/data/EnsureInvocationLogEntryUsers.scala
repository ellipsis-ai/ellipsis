package data

import javax.inject._

import services.DataService
import drivers.SlickPostgresDriver.api._
import models.accounts.linkedaccount.{LinkedAccountQueries, RawLinkedAccount}
import models.behaviors.invocationlogentry.InvocationLogEntryQueries
import play.api.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EnsureInvocationLogEntryUsers @Inject() (dataService: DataService) {

  def runFor(accounts: List[RawLinkedAccount]): Future[Unit] = {
    if (accounts.isEmpty) {
      Future.successful({})
    } else {
      val account = accounts.head
      val query =
        InvocationLogEntryQueries.all.
          filter(_.maybeUserId.isEmpty).
          filter(_.context === account.loginInfo.providerID).
          filter(_.maybeUserIdForContext === account.loginInfo.providerKey).
          map(_.maybeUserId)
      dataService.run(query.update(Some(account.userId))).flatMap { count =>
        Logger.info(s"updating logs for ${account.loginInfo.providerID}/${account.loginInfo.providerKey}: $count")
        runFor(accounts.tail)
      }
    }
  }

  def run(): Unit = {
    dataService.runNow(
      dataService.run(LinkedAccountQueries.all.result).flatMap { accounts =>
        runFor(accounts.toList)
      }.map(_ => {})
    )
  }

  run()
}
