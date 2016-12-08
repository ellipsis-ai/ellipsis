package models.behaviors.invocationlogentry

import javax.inject.Inject

import com.google.inject.Provider
import models.IDs
import models.behaviors.BotResult
import models.behaviors.behaviorversion.{BehaviorVersion, BehaviorVersionQueries}
import models.behaviors.events.MessageEvent
import models.team.Team
import org.joda.time.LocalDateTime
import services.DataService
import drivers.SlickPostgresDriver.api._

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
                                  createdAt: LocalDateTime
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
  def createdAt = column[LocalDateTime]("created_at")

  def * = (id, behaviorVersionId, resultType, messageText, resultText, context, maybeUserIdForContext, runtimeInMilliseconds, createdAt) <>
    ((RawInvocationLogEntry.apply _).tupled, RawInvocationLogEntry.unapply _)
}

class InvocationLogEntryServiceImpl @Inject() (
                                             dataServiceProvider: Provider[DataService]
                                           ) extends InvocationLogEntryService {

  def dataService = dataServiceProvider.get

  val all = TableQuery[InvocationLogEntriesTable]
  val allWithVersion = all.join(BehaviorVersionQueries.allWithBehavior).on(_.behaviorVersionId === _._1._1.id)

  val truncateDate = SimpleFunction.binary[String, LocalDateTime, LocalDateTime]("date_trunc")

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

  def countsForDate(date: LocalDateTime): Future[Seq[(String, Int)]] = {
    val action = allWithVersion.
      filter { case(entry, _) => truncateDate("day", entry.createdAt) === truncateDate("day", date) }.
      groupBy { case(entry, ((version, _), ((behavior, team), _))) => team.id }.
      map { case(teamId, q) =>
        (teamId, q.length)
      }.
      result
    dataService.run(action)
  }

  def uniqueInvokingUserCountsForDate(date: LocalDateTime): Future[Seq[(String, Int)]] = {
    val action = allWithVersion.
      filter { case(entry, _) => truncateDate("day", entry.createdAt) === truncateDate("day", date) }.
      groupBy { case(entry, ((version, _), ((behavior, team), _))) => (team.id, entry.maybeUserIdForContext.getOrElse("<no user>")) }.
      map { case((teamId, userId), q) =>
        (teamId, userId, 1)
      }.
      groupBy { case(teamId, _, _) => teamId }.
      map { case(teamId, q) => (teamId, q.map(_._3).sum.getOrElse(0)) }.
      result
    dataService.run(action)
  }

  def uniqueInvokedBehaviorCountsForDate(date: LocalDateTime): Future[Seq[(String, Int)]] = {
    val action = allWithVersion.
      filter { case(entry, _) => truncateDate("day", entry.createdAt) === truncateDate("day", date) }.
      groupBy { case(entry, ((version, _), ((behavior, team), _))) => (team.id, behavior.id) }.
      map { case((teamId, behaviorId), q) =>
        (teamId, behaviorId, 1)
      }.
      groupBy { case(teamId, _, _) => teamId }.
      map { case(teamId, q) => (teamId, q.map(_._3).sum.getOrElse(0)) }.
      result
    dataService.run(action)
  }

  def uncompiledForTeamForDateQuery(teamId: Rep[String], date: Rep[LocalDateTime]) = {
    allWithVersion.
      filter { case(entry, ((version, user), ((behavior, team), _))) => teamId === team.id}.
      filter { case(entry, _) => truncateDate("day", entry.createdAt) === date }
  }
  val forTeamForDateQuery = Compiled(uncompiledForTeamForDateQuery _)

  def forTeamForDate(team: Team, date: LocalDateTime): Future[Seq[InvocationLogEntry]] = {
    val action = forTeamForDateQuery(team.id, date).result.map { r =>
      r.map(tuple2Entry)
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
        LocalDateTime.now
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
