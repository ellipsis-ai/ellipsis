package models.accounts.logintoken

import java.time.OffsetDateTime
import javax.inject.Inject

import com.google.inject.Provider
import models.accounts.user.User
import models.IDs
import services.DataService
import drivers.SlickPostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class LoginTokensTable(tag: Tag) extends Table[LoginToken](tag, "login_tokens") {
  def value = column[String]("value")
  def userId = column[String]("user_id")
  def createdAt = column[OffsetDateTime]("created_at")

  def * = (value, userId, createdAt) <> ((LoginToken.apply _).tupled, LoginToken.unapply _)
}

class LoginTokenServiceImpl @Inject() (dataServiceProvider: Provider[DataService]) extends LoginTokenService {

  def dataService = dataServiceProvider.get

  val all = TableQuery[LoginTokensTable]

  def uncompiledFindQuery(value: Rep[String]) = {
    all.filter(_.value === value)
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  def find(value: String): Future[Option[LoginToken]] = {
    dataService.run(findQuery(value).result.map(_.headOption))
  }

  def createForAction(user: User): DBIO[LoginToken] = {
    val instance = LoginToken(IDs.next, user.id, OffsetDateTime.now)
    (all += instance).map(_ => instance)
  }

}
