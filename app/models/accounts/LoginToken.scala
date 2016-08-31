package models.accounts

import com.github.tototoshi.slick.PostgresJodaSupport._
import models.IDs
import org.joda.time.DateTime
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global

case class LoginToken(
                        value: String,
                        userId: String,
                        isUsed: Boolean,
                        createdAt: DateTime
                      ) {

  def isExpired: Boolean = createdAt.isBefore(LoginTokenQueries.expiryCutoff)

  def isValid: Boolean = !isUsed && !isExpired

  def use: DBIO[Unit] = LoginTokenQueries.use(this)

}

class LoginTokensTable(tag: Tag) extends Table[LoginToken](tag, "login_tokens") {
  def value = column[String]("value")
  def userId = column[String]("user_id")
  def isUsed = column[Boolean]("is_used")
  def createdAt = column[DateTime]("created_at")

  def * = (value, userId, isUsed, createdAt) <> ((LoginToken.apply _).tupled, LoginToken.unapply _)
}

object LoginTokenQueries {
  val all = TableQuery[LoginTokensTable]

  def uncompiledFindQuery(value: Rep[String]) = {
    all.filter(_.value === value)
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  def find(value: String): DBIO[Option[LoginToken]] = {
    findQuery(value).result.map(_.headOption)
  }

  val EXPIRY_SECONDS = 300

  def expiryCutoff: DateTime = DateTime.now.minusSeconds(EXPIRY_SECONDS)

  def use(loginToken: LoginToken): DBIO[Unit] = {
    all.filter(_.value === loginToken.value).map(_.isUsed).update(true).map(_ => Unit)
  }

  def createFor(user: User): DBIO[LoginToken] = {
    val instance = LoginToken(IDs.next, user.id, isUsed = false, DateTime.now)
    (all += instance).map(_ => instance)
  }

}
