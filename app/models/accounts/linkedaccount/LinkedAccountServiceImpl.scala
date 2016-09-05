package models.accounts.linkedaccount

import javax.inject._

import com.github.tototoshi.slick.PostgresJodaSupport._
import com.mohiva.play.silhouette.api.LoginInfo
import models.accounts.{SlackProfileQueries, SlackProvider}
import models.accounts.user.{User, UserQueries}
import org.joda.time.DateTime
import services.DataService
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RawLinkedAccount(userId: String, loginInfo: LoginInfo, createdAt: DateTime)

class LinkedAccountsTable(tag: Tag) extends Table[RawLinkedAccount](tag, "linked_accounts") {
  def userId = column[String]("user_id")
  def providerId = column[String]("provider_id")
  def providerKey = column[String]("provider_key")
  def createdAt = column[DateTime]("created_at")

  def loginInfo = (providerId, providerKey) <> (LoginInfo.tupled, LoginInfo.unapply _)
  def * = (userId, loginInfo, createdAt) <> (RawLinkedAccount.tupled, RawLinkedAccount.unapply _)
}

class LinkedAccountServiceImpl @Inject() (dataServiceProvider: Provider[DataService]) extends LinkedAccountService {

  def dataService = dataServiceProvider.get

  val all = TableQuery[LinkedAccountsTable]
  val joined = all.join(UserQueries.all).on(_.userId === _.id)

  def uncompiledFindQuery(providerId: Rep[String], providerKey: Rep[String], teamId: Rep[String]) = {
    joined.
      filter { case(linked, user) => linked.providerId === providerId }.
      filter { case(linked, user) => linked.providerKey === providerKey }.
      filter { case(linked, user) => user.teamId === teamId }
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  def find(loginInfo: LoginInfo, teamId: String): Future[Option[LinkedAccount]] = {
    val action = findQuery(loginInfo.providerID, loginInfo.providerKey, teamId).
      result.
      map { result =>
        result.headOption.map(tuple2LinkedAccount)
      }
    dataService.run(action)
  }

  def save(link: LinkedAccount): Future[LinkedAccount] = {
    val query = all.filter(_.providerId === link.loginInfo.providerID).filter(_.providerKey === link.loginInfo.providerKey)
    val action = query.result.headOption.flatMap {
      case Some(_) => {
        query.
          update(link.toRaw)
      }
      case None => all += link.toRaw
    }.map { _ => link }
    dataService.run(action)
  }

  def tuple2LinkedAccount(tuple: (RawLinkedAccount, User)): LinkedAccount = {
    LinkedAccount(tuple._2, tuple._1.loginInfo, tuple._1.createdAt)
  }

  def uncompiledAllForQuery(userId: Rep[String]) = {
    joined.filter { case(_, u) => u.id === userId }
  }
  val allForQuery = Compiled(uncompiledAllForQuery _)

  def allFor(user: User): Future[Seq[LinkedAccount]] = {
    val action = allForQuery(user.id).
      result.
      map { result =>
        result.map(tuple2LinkedAccount)
      }
    dataService.run(action)
  }

  def uncompiledForSlackForQuery(userId: Rep[String]) = {
    joined.
      filter { case(la, u) => la.providerId === SlackProvider.ID }.
      filter { case(la, u) => u.id === userId }
  }
  val forSlackForQuery = Compiled(uncompiledForSlackForQuery _)

  def maybeForSlackFor(user: User): Future[Option[LinkedAccount]] = {
    val action = forSlackForQuery(user.id).result.map { r =>
      r.headOption.map(tuple2LinkedAccount)
    }
    dataService.run(action)
  }

  def isAdmin(linkedAccount: LinkedAccount): Future[Boolean] = {
    val action = SlackProfileQueries.find(linkedAccount.loginInfo).map { maybeId =>
      maybeId.contains(ELLIPSIS_SLACK_TEAM_ID)
    }
    dataService.run(action)
  }

  val ELLIPSIS_SLACK_TEAM_ID = "T0LP53H0A"

}
