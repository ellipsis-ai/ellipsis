package models.accounts

import com.github.tototoshi.slick.PostgresJodaSupport._
import javax.inject.{Inject, Singleton}

import models.accounts.user.User
import models.{IDs, Models}
import org.joda.time.DateTime
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class LoginTokensTable(tag: Tag) extends Table[LoginToken](tag, "login_tokens") {
  def value = column[String]("value")
  def userId = column[String]("user_id")
  def isUsed = column[Boolean]("is_used")
  def createdAt = column[DateTime]("created_at")

  def * = (value, userId, isUsed, createdAt) <> ((LoginToken.apply _).tupled, LoginToken.unapply _)
}

@Singleton
class LoginTokenServiceImpl @Inject() (models: Models) extends LoginTokenService {
  val all = TableQuery[LoginTokensTable]

  def uncompiledFindQuery(value: Rep[String]) = {
    all.filter(_.value === value)
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  def find(value: String): Future[Option[LoginToken]] = {
    models.run(findQuery(value).result.map(_.headOption))
  }

  def use(loginToken: LoginToken): Future[Unit] = {
    models.run(all.filter(_.value === loginToken.value).map(_.isUsed).update(true).map(_ => Unit))
  }

  def createFor(user: User): Future[LoginToken] = {
    val instance = LoginToken(IDs.next, user.id, isUsed = false, DateTime.now)
    models.run((all += instance).map(_ => instance))
  }

}
