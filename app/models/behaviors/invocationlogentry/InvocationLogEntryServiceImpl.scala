package models.behaviors.invocationlogentry

import javax.inject.Inject

import com.github.tototoshi.slick.PostgresJodaSupport._
import com.google.inject.Provider
import models.IDs
import models.behaviors.BotResult
import models.behaviors.behaviorversion.{BehaviorVersion, BehaviorVersionQueries}
import models.behaviors.events.MessageEvent
import org.joda.time.DateTime
import services.DataService
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class InvocationLogEntriesTable(tag: Tag) extends Table[InvocationLogEntry](tag, "invocation_log_entries") {

  def id = column[String]("id", O.PrimaryKey)
  def behaviorVersionId = column[String]("behavior_version_id")
  def resultType = column[String]("result_type")
  def messageText = column[String]("message_text")
  def resultText = column[String]("result_text")
  def context = column[String]("context")
  def maybeUserIdForContext = column[Option[String]]("user_id_for_context")
  def runtimeInMilliseconds = column[Long]("runtime_in_milliseconds")
  def createdAt = column[DateTime]("created_at")

  def * = (id, behaviorVersionId, resultType, messageText, resultText, context, maybeUserIdForContext, runtimeInMilliseconds, createdAt) <>
    ((InvocationLogEntry.apply _).tupled, InvocationLogEntry.unapply _)
}

class InvocationLogEntryServiceImpl @Inject() (
                                             dataServiceProvider: Provider[DataService]
                                           ) extends InvocationLogEntryService {

  def dataService = dataServiceProvider.get

  val all = TableQuery[InvocationLogEntriesTable]
  val allWithVersion = all.join(BehaviorVersionQueries.allWithBehavior).on(_.behaviorVersionId === _._1._1.id)

  val truncateDate = SimpleFunction.binary[String, DateTime, DateTime]("date_trunc")

  def countsByDay: Future[Seq[(DateTime, String, Int)]] = {
    val action = allWithVersion.
      map { case(entry, ((version, _), (behavior, team))) =>
        (truncateDate("day", entry.createdAt), team.id, 1)
      }.
      groupBy { case(date, teamId, _) => (date, teamId)}.
      map { case((date, teamId), q) =>
        (date, teamId, q.map(_._3).sum.getOrElse(0))
      }.
      result
    dataService.run(action)
  }

  def createFor(
                 behaviorVersion: BehaviorVersion,
                 result: BotResult,
                 event: MessageEvent,
                 maybeUserIdForContext: Option[String],
                 runtimeInMilliseconds: Long
               ): Future[InvocationLogEntry] = {
    val newInstance =
      InvocationLogEntry(
        IDs.next,
        behaviorVersion.id,
        result.resultType.toString,
        event.context.fullMessageText,
        result.fullText,
        event.context.name,
        maybeUserIdForContext,
        runtimeInMilliseconds,
        DateTime.now
      )

    val action = (all += newInstance).map(_ => newInstance)
    dataService.run(action)
  }
}
