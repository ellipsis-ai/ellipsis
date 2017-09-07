package models.accounts.linkedaccount

import java.time.OffsetDateTime
import javax.inject._

import com.mohiva.play.silhouette.api.LoginInfo
import models.accounts.user.User
import services.DataService
import drivers.SlickPostgresDriver.api._

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
                                           implicit val ec: ExecutionContext
                                         ) extends LinkedAccountService {

  def dataService = dataServiceProvider.get

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
    val query = all.filter(_.providerId === link.loginInfo.providerID).filter(_.providerKey === link.loginInfo.providerKey)
    query.result.headOption.flatMap {
      case Some(_) => {
        query.
          update(link.toRaw)
      }
      case None => all += link.toRaw
    }.map { _ => link }
  }

  def save(link: LinkedAccount): Future[LinkedAccount] = {
    dataService.run(saveAction(link))
  }

  def allFor(user: User): Future[Seq[LinkedAccount]] = {
    val action = allForQuery(user.id).
      result.
      map { result =>
        result.map(tuple2LinkedAccount)
      }
    dataService.run(action)
  }

  def maybeForSlackForAction(user: User): DBIO[Option[LinkedAccount]] = {
    forSlackForQuery(user.id).result.map { r =>
      r.headOption.map(tuple2LinkedAccount)
    }
  }

  def maybeForSlackFor(user: User): Future[Option[LinkedAccount]] = {
    dataService.run(maybeForSlackForAction(user))
  }

  def isAdminAction(linkedAccount: LinkedAccount): DBIO[Boolean] = {
    dataService.slackProfiles.findAction(linkedAccount.loginInfo).map { maybeProfile =>
      maybeProfile.map(_.teamId).contains(LinkedAccount.ELLIPSIS_SLACK_TEAM_ID)
    }
  }

  def isAdmin(linkedAccount: LinkedAccount): Future[Boolean] = {
    dataService.run(isAdminAction(linkedAccount))
  }

}
