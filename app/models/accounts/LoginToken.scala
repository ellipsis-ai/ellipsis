package models.accounts

import com.github.tototoshi.slick.PostgresJodaSupport._
import models.IDs
import org.joda.time.DateTime
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global

case class LoginToken(
                        value: String,
                        userId: String,
                        createdAt: DateTime
                      ) {

  def isExpired: Boolean = createdAt.isBefore(LoginTokenQueries.expiryCutoff)

  def isValid: Boolean = !isExpired

}

class LoginTokensTable(tag: Tag) extends Table[LoginToken](tag, "login_tokens") {
  def value = column[String]("value")
  def userId = column[String]("user_id")
  def createdAt = column[DateTime]("created_at")

  def * = (value, userId, createdAt) <> ((LoginToken.apply _).tupled, LoginToken.unapply _)
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

  val EXPIRY_SECONDS = 60

  def expiryCutoff: DateTime = DateTime.now.minusSeconds(EXPIRY_SECONDS)

  def createFor(user: User): DBIO[LoginToken] = {
    val instance = LoginToken(IDs.next, user.id, DateTime.now)
    (all += instance).map(_ => instance)
  }

}
