package models.accounts.linkedaccount

import java.time.OffsetDateTime

import akka.actor.ActorSystem
import com.mohiva.play.silhouette.api.LoginInfo
import drivers.SlickPostgresDriver.api._
import javax.inject._
import models.accounts.github.GithubProvider
import models.accounts.slack.SlackProvider
import models.accounts.user.User
import models.accounts.{MSAzureActiveDirectoryContext, MSTeamsContext}
import services.DataService
import services.caching.CacheService

import scala.concurrent.{ExecutionContext, Future}

case class RawLinkedAccount(userId: String, loginInfo: LoginInfo, createdAt: OffsetDateTime)

class LinkedAccountsTable(tag: Tag) extends Table[RawLinkedAccount](tag, "linked_accounts") {
  def userId = column[String]("user_id")
  def providerId = column[String]("provider_id")
  def providerKey = column[String]("provider_key")
  def createdAt = column[OffsetDateTime]("created_at")

  def loginInfo = (providerId, providerKey) <> (LoginInfo.tupled, LoginInfo.unapply _)
  def * = (userId, loginInfo, createdAt) <> (RawLinkedAccount.tupled, RawLinkedAccount.unapply _)
}

class LinkedAccountServiceImpl @Inject() (
                                           dataServiceProvider: Provider[DataService],
                                           cacheServiceProvider: Provider[CacheService],
                                           implicit val ec: ExecutionContext,
                                           implicit val actorSystem: ActorSystem
                                         ) extends LinkedAccountService {

  def dataService = dataServiceProvider.get
  def cacheService = cacheServiceProvider.get

  import LinkedAccountQueries._

  def findAction(loginInfo: LoginInfo, teamId: String): DBIO[Option[LinkedAccount]] = {
    findQuery(loginInfo.providerID, loginInfo.providerKey, teamId).
      result.
      map { result =>
        result.headOption.map(tuple2LinkedAccount)
      }
  }

  def find(loginInfo: LoginInfo, teamId: String): Future[Option[LinkedAccount]] = {
    dataService.run(findAction(loginInfo, teamId))
  }

  def saveAction(link: LinkedAccount): DBIO[LinkedAccount] = {
    val query =
      all.
        filter(_.providerId === link.loginInfo.providerID).
        filter(_.providerKey === link.loginInfo.providerKey).
        filter(_.userId === link.user.id)
    query.result.headOption.flatMap {
      case Some(_) => DBIO.successful({})
      case None => all += link.toRaw
    }.map { _ => link }
  }

  def save(link: LinkedAccount): Future[LinkedAccount] = {
    dataService.run(saveAction(link))
  }

  def allForAction(user: User): DBIO[Seq[LinkedAccount]] = {
    allForQuery(user.id).
      result.
      map { result =>
        result.map(tuple2LinkedAccount)
      }
  }

  def allFor(user: User): Future[Seq[LinkedAccount]] = {
    dataService.run(allForAction(user))
  }

  def allForLoginInfoAction(loginInfo: LoginInfo): DBIO[Seq[LinkedAccount]] = {
    allForLoginInfoQuery(loginInfo.providerID, loginInfo.providerKey).result.map { r =>
      r.map(tuple2LinkedAccount)
    }
  }

  def allForLoginInfo(loginInfo: LoginInfo): Future[Seq[LinkedAccount]] = {
    dataService.run(allForLoginInfoAction(loginInfo))
  }

  def maybeForSlackForAction(user: User): DBIO[Option[LinkedAccount]] = {
    forProviderForQuery(user.id, SlackProvider.ID).result.map { r =>
      r.headOption.map(tuple2LinkedAccount)
    }
  }

  def maybeForSlackFor(user: User): Future[Option[LinkedAccount]] = {
    dataService.run(maybeForSlackForAction(user))
  }

  def maybeForMSTeamsFor(user: User): Future[Option[LinkedAccount]] = {
    val action = forProviderForQuery(user.id, MSTeamsContext.toString).result.map { r =>
      r.headOption.map(tuple2LinkedAccount)
    }
    dataService.run(action)
  }

  def maybeForMSAzureActiveDirectoryForAction(user: User): DBIO[Option[LinkedAccount]] = {
    forProviderForQuery(user.id, MSAzureActiveDirectoryContext.toString).result.map { r =>
      r.headOption.map(tuple2LinkedAccount)
    }
  }

  def maybeForMSAzureActiveDirectoryFor(user: User): Future[Option[LinkedAccount]] = {
    dataService.run(maybeForMSAzureActiveDirectoryForAction(user))
  }

  def maybeForGithubFor(user: User): Future[Option[LinkedAccount]] = {
    val action = forProviderForQuery(user.id, GithubProvider.ID).result.map { r =>
      r.headOption.map(tuple2LinkedAccount)
    }
    dataService.run(action)
  }

  def deleteGithubFor(user: User): Future[Boolean] = {
    val action = rawForProviderForQuery(user.id, GithubProvider.ID).delete.map(_ > 0)
    dataService.run(action)
  }

}
