package models

import com.github.tototoshi.slick.PostgresJodaSupport._
import com.mohiva.play.silhouette.api.LoginInfo
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
  val joined = all.join(User.all).on(_.userId === _.id)

  def uncompiledFindQuery(providerId: Rep[String], providerKey: Rep[String]) = {
    joined.
      filter { case(linked, user) => linked.providerId === providerId }.
      filter { case(linked, user) => linked.providerKey === providerKey }
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  def find(loginInfo: LoginInfo): DBIO[Option[LinkedAccount]] = {
    findQuery(loginInfo.providerID, loginInfo.providerKey).
      result.
      map { result =>
      result.headOption.map(tuple2LinkedAccount)
    }
  }

  def save(link: LinkedAccount): DBIO[LinkedAccount] = {
    val query = all.filter(_.providerId === link.loginInfo.providerID).filter(_.providerKey === link.loginInfo.providerKey)
    query.result.headOption.flatMap { maybeLink =>
      maybeLink match {
        case Some(_) => {
          query.
            update(link.raw)
        }
        case None => all += link.raw
      }
    }.map { _ => link }
  }

  def tuple2LinkedAccount(tuple: (RawLinkedAccount, User)): LinkedAccount = {
    LinkedAccount(tuple._2, tuple._1.loginInfo, tuple._1.createdAt)
  }

  def uncompiledAllForQuery(providerId: Rep[String], userId: Rep[String]) = {
    joined.
      filter { case(_, u) => u.id === userId }.
      filter { case(rawLinked, _) => rawLinked.providerId === providerId }
  }
  val allForQuery = Compiled(uncompiledAllForQuery _)

  def allFor(providerId: String, maybeUser: Option[User]): DBIO[Seq[LinkedAccount]] = {
    maybeUser.map { user =>
      allForQuery(providerId, user.id).
        result.
        map { result =>
        result.map(tuple2LinkedAccount)
      }
    }.getOrElse(DBIO.successful(Seq()))
  }

  def allFor(providerId: String): DBIO[Seq[LinkedAccount]] = {
    joined.
      filter { case(rawLinked, _) => rawLinked.providerId === providerId }.
      result.
      map { result => result.map(tuple2LinkedAccount) }
  }

  def uncompiledMostRecentForQuery(providerId: Rep[String], userId: Rep[String]) = {
    uncompiledAllForQuery(providerId, userId)
      .sortBy { case(linkedAccount, user) =>
      linkedAccount.createdAt.desc
    }.
      take(1)
  }
  val mostRecentForQuery = Compiled(uncompiledMostRecentForQuery _)

  def findMostRecentFor(providerId: String, maybeUser: Option[User]): DBIO[Option[LinkedAccount]] = {
    maybeUser.map { user =>
      mostRecentForQuery(providerId, user.id).result.map { linkedAccounts =>
        linkedAccounts.headOption.map(tuple2LinkedAccount)
      }
    }.getOrElse(DBIO.successful(None))
  }
}
