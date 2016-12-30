package models.behaviors.invocationtoken

import javax.inject.Inject

import com.google.inject.Provider
import models.IDs
import org.joda.time.DateTime
import services.DataService
import drivers.SlickPostgresDriver.api._
import models.accounts.user.User
import models.behaviors.behavior.Behavior

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class InvocationTokensTable(tag: Tag) extends Table[InvocationToken](tag, "invocation_tokens") {

  def id = column[String]("id", O.PrimaryKey)
  def userId = column[String]("user_id")
  def behaviorId = column[String]("behavior_id")
  def createdAt = column[DateTime]("created_at")

  def * = (id, userId, behaviorId, createdAt) <> ((InvocationToken.apply _).tupled, InvocationToken.unapply _)
}

class InvocationTokenServiceImpl @Inject() (
                                  dataServiceProvider: Provider[DataService]
                                ) extends InvocationTokenService {

  def dataService = dataServiceProvider.get

  val all = TableQuery[InvocationTokensTable]

  def uncompiledFindQueryFor(id: Rep[String]) = {
    all.filter(_.id === id)
  }
  val findQueryFor = Compiled(uncompiledFindQueryFor _)

  def find(id: String): Future[Option[InvocationToken]] = {
    dataService.run(findQueryFor(id).result.map(_.headOption))
  }

  def createFor(user: User, behavior: Behavior): Future[InvocationToken] = {
    val newInstance = InvocationToken(IDs.next, user.id, behavior.id, DateTime.now)
    dataService.run((all += newInstance).map(_ => newInstance))
  }

}
