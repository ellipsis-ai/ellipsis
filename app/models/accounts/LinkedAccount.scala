package models.accounts

import com.github.tototoshi.slick.PostgresJodaSupport._
import com.mohiva.play.silhouette.api.LoginInfo
import models.Team
import models.accounts.user.{User, UserQueries}
import org.joda.time.DateTime
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global

case class LinkedAccount(user: User, loginInfo: LoginInfo, createdAt: DateTime) {
  def raw: RawLinkedAccount = RawLinkedAccount(user.id, loginInfo, createdAt)
  def save = LinkedAccount.save(this)

  def maybeFullToken: DBIO[Option[OAuth2Token]] = {
    OAuth2Token.maybeFullFor(loginInfo)
  }

  def maybeMyToken: DBIO[Option[OAuth2Token]] = {
    OAuth2Token.findByLoginInfo(loginInfo)
  }

  def maybeSlackProfile: DBIO[Option[SlackProfile]] = {
    SlackProfileQueries.find(loginInfo)
  }

  def maybeSlackTeamId: DBIO[Option[String]] = maybeSlackProfile.map(_.map(_.teamId))

  def isAdmin: DBIO[Boolean] = maybeSlackTeamId.map { maybeId =>
    maybeId.contains(LinkedAccount.ELLIPSIS_SLACK_TEAM_ID)
  }

}

case class RawLinkedAccount(userId: String, loginInfo: LoginInfo, createdAt: DateTime)

class LinkedAccountsTable(tag: Tag) extends Table[RawLinkedAccount](tag, "linked_accounts") {
  def userId = column[String]("user_id")
  def providerId = column[String]("provider_id")
  def providerKey = column[String]("provider_key")
  def createdAt = column[DateTime]("created_at")

  def loginInfo = (providerId, providerKey) <> (LoginInfo.tupled, LoginInfo.unapply _)
  def * = (userId, loginInfo, createdAt) <> (RawLinkedAccount.tupled, RawLinkedAccount.unapply _)
}

object LinkedAccount {
  val all = TableQuery[LinkedAccountsTable]
  val joined = all.join(UserQueries.all).on(_.userId === _.id)

  def uncompiledFindQuery(providerId: Rep[String], providerKey: Rep[String], teamId: Rep[String]) = {
    joined.
      filter { case(linked, user) => linked.providerId === providerId }.
      filter { case(linked, user) => linked.providerKey === providerKey }.
      filter { case(linked, user) => user.teamId === teamId }
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  def find(loginInfo: LoginInfo, teamId: String): DBIO[Option[LinkedAccount]] = {
    findQuery(loginInfo.providerID, loginInfo.providerKey, teamId).
      result.
      map { result =>
      result.headOption.map(tuple2LinkedAccount)
    }
  }

  def save(link: LinkedAccount): DBIO[LinkedAccount] = {
    val query = all.filter(_.providerId === link.loginInfo.providerID).filter(_.providerKey === link.loginInfo.providerKey)
    query.result.headOption.flatMap {
      case Some(_) => {
        query.
          update(link.raw)
      }
      case None => all += link.raw
    }.map { _ => link }
  }

  def tuple2LinkedAccount(tuple: (RawLinkedAccount, User)): LinkedAccount = {
    LinkedAccount(tuple._2, tuple._1.loginInfo, tuple._1.createdAt)
  }

  def uncompiledAllForQuery(userId: Rep[String]) = {
    joined.filter { case(_, u) => u.id === userId }
  }
  val allForQuery = Compiled(uncompiledAllForQuery _)

  def allFor(user: User): DBIO[Seq[LinkedAccount]] = {
    allForQuery(user.id).
      result.
      map { result =>
      result.map(tuple2LinkedAccount)
    }
  }

  def uncompiledForSlackForQuery(userId: Rep[String]) = {
    joined.
      filter { case(la, u) => la.providerId === SlackProvider.ID }.
      filter { case(la, u) => u.id === userId }
  }
  val forSlackForQuery = Compiled(uncompiledForSlackForQuery _)

  def maybeForSlackFor(user: User): DBIO[Option[LinkedAccount]] = {
    forSlackForQuery(user.id).result.map { r =>
      r.headOption.map(tuple2LinkedAccount)
    }
  }

  val ELLIPSIS_SLACK_TEAM_ID = "T0LP53H0A"

}
