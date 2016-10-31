package models.behaviors.invocationlogentry

import javax.inject.Inject

import com.github.nscala_time.time.OrderingImplicits.DateTimeOrdering
import com.github.tototoshi.slick.PostgresJodaSupport._
import com.google.inject.Provider
import models.IDs
import models.behaviors.BotResult
import models.behaviors.behaviorversion.{BehaviorVersion, BehaviorVersionQueries}
import models.behaviors.events.MessageEvent
import models.team.Team
import org.joda.time.DateTime
import services.DataService
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RawInvocationLogEntry(
                                  id: String,
                                  behaviorVersionId: String,
                                  resultType: String,
                                  messageText: String,
                                  resultText: String,
                                  context: String,
                                  maybeUserIdForContext: Option[String],
                                  runtimeInMilliseconds: Long,
                                  createdAt: DateTime
                                )

class InvocationLogEntriesTable(tag: Tag) extends Table[RawInvocationLogEntry](tag, "invocation_log_entries") {

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
    ((RawInvocationLogEntry.apply _).tupled, RawInvocationLogEntry.unapply _)
}

class InvocationLogEntryServiceImpl @Inject() (
                                             dataServiceProvider: Provider[DataService]
                                           ) extends InvocationLogEntryService {

  def dataService = dataServiceProvider.get

  val all = TableQuery[InvocationLogEntriesTable]
  val allWithVersion = all.join(BehaviorVersionQueries.allWithBehavior).on(_.behaviorVersionId === _._1._1.id)

  val truncateDate = SimpleFunction.binary[String, DateTime, DateTime]("date_trunc")

  type TupleType = (RawInvocationLogEntry, BehaviorVersionQueries.TupleType)

  def tuple2Entry(tuple: TupleType): InvocationLogEntry = {
    val raw = tuple._1
    InvocationLogEntry(
      raw.id,
      BehaviorVersionQueries.tuple2BehaviorVersion(tuple._2),
      raw.resultType,
      raw.messageText,
      raw.resultText,
      raw.context,
      raw.maybeUserIdForContext,
      raw.runtimeInMilliseconds,
      raw.createdAt
    )
  }

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

  def uniqueInvokingUserCountsByDay: Future[Seq[(DateTime, String, Int)]] = {
    val action = allWithVersion.
      map { case(entry, ((version, _), (behavior, team))) =>
        (truncateDate("day", entry.createdAt), team.id, entry.maybeUserIdForContext.getOrElse("None"))
      }.
      groupBy { case(date, teamId, userId) => (date, teamId, userId)}.
      map { case((date, teamId, userId), q) =>
        (date, teamId, userId, 1)
      }.
      groupBy { case(date, teamId, _, _) => (date, teamId) }.
      map { case((date, teamId), q) => (date, teamId, q.map(_._4).sum.getOrElse(0)) }.
      result
    dataService.run(action)
  }

  def uniqueInvokedBehaviorCountsByDay: Future[Seq[(DateTime, String, Int)]] = {
    val action = allWithVersion.
      map { case(entry, ((version, _), (behavior, team))) =>
        (truncateDate("day", entry.createdAt), team.id, behavior.id)
      }.
      groupBy { case(date, teamId, behaviorId) => (date, teamId, behaviorId)}.
      map { case((date, teamId, behaviorId), q) =>
        (date, teamId, behaviorId, 1)
      }.
      groupBy { case(date, teamId, _, _) => (date, teamId) }.
      map { case((date, teamId), q) => (date, teamId, q.map(_._4).sum.getOrElse(0)) }.
      result
    dataService.run(action)
  }

  def uncompiledForTeamQuery(teamId: Rep[String]) = {
    allWithVersion.filter { case(entry, ((version, user), (behavior, t))) => teamId === t.id}
  }
  val forTeamQuery = Compiled(uncompiledForTeamQuery _)

  def forTeamByDay(team: Team): Future[Seq[(DateTime, Seq[InvocationLogEntry])]] = {
    val action = forTeamQuery(team.id).result.map { r =>
      r.
        map(tuple2Entry).
        groupBy(_.createdAt.withTimeAtStartOfDay).
        toSeq.
        sortBy { case(date, entry) => date }.
        reverse
    }
    dataService.run(action)
  }

  def createFor(
                 behaviorVersion: BehaviorVersion,
                 result: BotResult,
                 event: MessageEvent,
                 maybeUserIdForContext: Option[String],
                 runtimeInMilliseconds: Long
               ): Future[InvocationLogEntry] = {
    val raw =
      RawInvocationLogEntry(
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

    val action = (all += raw).map { _ =>
      InvocationLogEntry(
        raw.id,
        behaviorVersion,
        raw.resultType,
        raw.messageText,
        raw.resultText,
        raw.context,
        raw.maybeUserIdForContext,
        raw.runtimeInMilliseconds,
        raw.createdAt
      )
    }
    dataService.run(action)
  }
}
