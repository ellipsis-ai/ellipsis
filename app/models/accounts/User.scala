package models.accounts

import com.mohiva.play.silhouette.api.{Identity, LoginInfo}
import models.{Team, IDs}
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global

case class User(
                 id: String,
                 maybeEmail: Option[String]
                 ) extends Identity {

  def canAccess(team: Team): DBIO[Boolean] = {
    // TODO: something real
    DBIO.successful(true)
  }

  def loginInfo: LoginInfo = LoginInfo(User.EPHEMERAL_USER_ID, id)

  def save: DBIO[User] = User.save(this)
}

class UsersTable(tag: Tag) extends Table[User](tag, "users") {

  def id = column[String]("id", O.PrimaryKey)
  def maybeEmail = column[Option[String]]("email")

  def * =
    (id, maybeEmail) <> ((User.apply _).tupled, User.unapply _)
}

object User {
  val EPHEMERAL_USER_ID = "EPHEMERAL"

  val all = TableQuery[UsersTable]

  def empty: User = User(IDs.next, None)


  def uncompiledIsEmailTakenQuery(email: Rep[String]) = {
    all.filter(_.maybeEmail === email).exists
  }
  val isEmailTakenQuery = Compiled(uncompiledIsEmailTakenQuery _)

  def isEmailTaken(email: String): DBIO[Boolean] = {
    isEmailTakenQuery(email).result
  }

  def uncompiledFindQueryFor(id: Rep[String]) = {
    all.filter(_.id === id)
  }
  val findQueryFor = Compiled(uncompiledFindQueryFor _)

  def find(id: String): DBIO[Option[User]] = {
    findQueryFor(id).result.map(_.headOption)
  }

  def uncompiledFindByEmailQuery(email: Rep[String]) = all.filter(_.maybeEmail === email)
  val findByEmailQuery = Compiled(uncompiledFindByEmailQuery _)

  def findByEmail(email: String): DBIO[Option[User]] = findByEmailQuery(email).result.headOption

  def findBySlackUserId(slackUserId: String): DBIO[Option[User]] = {
    LinkedAccount.find(LoginInfo(SlackProvider.ID, slackUserId)).map { maybeLinkedAccount =>
      maybeLinkedAccount.map(_.user)
    }
  }

  def delete(user: User): DBIO[Option[User]] = {
    findQueryFor(user.id).delete.map{ _ => Some(user) }
  }

  def save(user: User): DBIO[User] = {
    val query = findQueryFor(user.id)
    query.result.flatMap { result =>
      result.headOption.map { existing =>
        all.filter(_.id === user.id).update(user)
      }.getOrElse {
        all += user
      }.map { _ => user }
    }
  }
}
