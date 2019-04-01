package models.behaviors.invocationtoken

import java.time.OffsetDateTime

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import javax.inject.Inject
import models.IDs
import models.accounts.user.User
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.scheduling.Scheduled
import models.behaviors.scheduling.scheduledmessage.ScheduledMessage
import services.{AWSLambdaService, DataService}

import scala.concurrent.{ExecutionContext, Future}

class InvocationTokensTable(tag: Tag) extends Table[InvocationToken](tag, "invocation_tokens") {

  def id = column[String]("id", O.PrimaryKey)
  def userId = column[String]("user_id")
  def behaviorVersionId = column[String]("behavior_version_id")
  def maybeScheduledMessageId = column[Option[String]]("scheduled_message_id")
  def maybeTeamIdForContext = column[Option[String]]("team_id_for_context")
  def createdAt = column[OffsetDateTime]("created_at")

  def * = (id, userId, behaviorVersionId, maybeScheduledMessageId, maybeTeamIdForContext, createdAt) <>
    ((InvocationToken.apply _).tupled, InvocationToken.unapply _)
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

  def createForAction(
                       user: User,
                       behaviorVersion: BehaviorVersion,
                       maybeScheduled: Option[Scheduled],
                       maybeTeamIdForContext: Option[String]
                     ): DBIO[InvocationToken] = {
    // TODO: Trying to write scheduled things here blows up because we sometimes delete those things, and the table uses it as a foreign key
    val newInstance = InvocationToken(IDs.next, user.id, behaviorVersion.id, None, maybeTeamIdForContext, OffsetDateTime.now)
    (all += newInstance).map(_ => newInstance)
  }

}
