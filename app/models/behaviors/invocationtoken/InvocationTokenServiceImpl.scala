package models.behaviors.invocationtoken

import javax.inject.Inject

import com.github.tototoshi.slick.PostgresJodaSupport._
import com.google.inject.Provider
import models.IDs
import models.team.Team
import org.joda.time.DateTime
import services.DataService
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class InvocationTokensTable(tag: Tag) extends Table[InvocationToken](tag, "invocation_tokens") {

  def id = column[String]("id", O.PrimaryKey)
  def teamId = column[String]("team_id")
  def isUsed = column[Boolean]("is_used")
  def createdAt = column[DateTime]("created_at")

  def * = (id, teamId, isUsed, createdAt) <> ((InvocationToken.apply _).tupled, InvocationToken.unapply _)
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

  def createFor(team: Team): Future[InvocationToken] = {
    val newInstance = InvocationToken(IDs.next, team.id, isUsed = false, DateTime.now)
    dataService.run((all += newInstance).map(_ => newInstance))
  }

  def use(token: InvocationToken): Future[InvocationToken] = {
    val action = all.filter(_.id === token.id).map(_.isUsed).update(true).map { _ =>
      token.copy(isUsed = true)
    }
    dataService.run(action)
  }

}
