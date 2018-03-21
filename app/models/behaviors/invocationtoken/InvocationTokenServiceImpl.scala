package models.behaviors.invocationtoken

import java.time.OffsetDateTime
import javax.inject.Inject

import com.google.inject.Provider
import models.IDs
import services.{AWSLambdaService, DataService}
import drivers.SlickPostgresDriver.api._
import models.accounts.user.User
import models.behaviors.behavior.Behavior
import models.behaviors.scheduling.Scheduled

import scala.concurrent.{ExecutionContext, Future}

class InvocationTokensTable(tag: Tag) extends Table[InvocationToken](tag, "invocation_tokens") {

  def id = column[String]("id", O.PrimaryKey)
  def userId = column[String]("user_id")
  def behaviorId = column[String]("behavior_id")
  def maybeScheduledMessageId = column[Option[String]]("scheduled_message_id")
  def createdAt = column[OffsetDateTime]("created_at")

  def * = (id, userId, behaviorId, maybeScheduledMessageId, createdAt) <> ((InvocationToken.apply _).tupled, InvocationToken.unapply _)
}

class InvocationTokenServiceImpl @Inject() (
                                  dataServiceProvider: Provider[DataService],
                                  lambdaServiceProvider: Provider[AWSLambdaService],
                                  implicit val ec: ExecutionContext
                                ) extends InvocationTokenService {

  def dataService = dataServiceProvider.get
  def lambdaService = lambdaServiceProvider.get

  val all = TableQuery[InvocationTokensTable]

  def uncompiledFindQueryFor(id: Rep[String]) = {
    all.filter(_.id === id)
  }
  val findQueryFor = Compiled(uncompiledFindQueryFor _)

  private def find(id: String): Future[Option[InvocationToken]] = {
    dataService.run(findQueryFor(id).result.map(_.headOption))
  }

  def isExpired(token: InvocationToken): Boolean = {
    // there can be a delay in starting, so we allow an extra 5 seconds
    token.createdAt.isBefore(OffsetDateTime.now.minusSeconds(lambdaService.invocationTimeoutSeconds + 5))
  }

  def findNotExpired(id: String): Future[Option[InvocationToken]] = {
    find(id).map { maybeToken =>
      maybeToken.filterNot(isExpired)
    }
  }

  def createForAction(user: User, behavior: Behavior, maybeScheduled: Option[Scheduled]): DBIO[InvocationToken] = {
    val newInstance = InvocationToken(IDs.next, user.id, behavior.id, maybeScheduled.map(_.id), OffsetDateTime.now)
    (all += newInstance).map(_ => newInstance)
  }

}
