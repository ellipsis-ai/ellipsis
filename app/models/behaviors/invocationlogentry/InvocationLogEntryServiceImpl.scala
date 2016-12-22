package models.behaviors.invocationlogentry

import javax.inject.Inject

import com.google.inject.Provider
import models.IDs
import models.behaviors.{BotResult, ParameterWithValue}
import models.behaviors.behaviorversion.{BehaviorVersion, BehaviorVersionQueries}
import models.behaviors.events.MessageEvent
import models.team.Team
import org.joda.time.DateTime
import services.DataService
import drivers.SlickPostgresDriver.api._
import models.behaviors.behavior.Behavior
import play.api.libs.json.{JsValue, Json}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RawInvocationLogEntry(
                                  id: String,
                                  behaviorVersionId: String,
                                  resultType: String,
                                  messageText: String,
                                  paramValues: JsValue,
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
  def paramValues = column[JsValue]("param_values")
  def resultText = column[String]("result_text")
  def context = column[String]("context")
  def maybeUserIdForContext = column[Option[String]]("user_id_for_context")
  def runtimeInMilliseconds = column[Long]("runtime_in_milliseconds")
  def createdAt = column[DateTime]("created_at")

  def * = (id, behaviorVersionId, resultType, messageText, paramValues, resultText, context, maybeUserIdForContext, runtimeInMilliseconds, createdAt) <>
    ((RawInvocationLogEntry.apply _).tupled, RawInvocationLogEntry.unapply _)
}

class InvocationLogEntryServiceImpl @Inject() (
                                             dataServiceProvider: Provider[DataService]
                                           ) extends InvocationLogEntryService {

  def dataService = dataServiceProvider.get

  val all = TableQuery[InvocationLogEntriesTable]
  val allWithVersion = all.join(BehaviorVersionQueries.allWithBehavior).on(_.behaviorVersionId === _._1._1.id)

  type TupleType = (RawInvocationLogEntry, BehaviorVersionQueries.TupleType)

  def tuple2Entry(tuple: TupleType): InvocationLogEntry = {
    val raw = tuple._1
    InvocationLogEntry(
      raw.id,
      BehaviorVersionQueries.tuple2BehaviorVersion(tuple._2),
      raw.resultType,
      raw.messageText,
      raw.paramValues,
      raw.resultText,
      raw.context,
      raw.maybeUserIdForContext,
      raw.runtimeInMilliseconds,
      raw.createdAt
    )
  }

  def uncompiledCountsForDateQuery(date: Rep[DateTime]) = {
    allWithVersion.
      filter { case(entry, _) => entry.createdAt.trunc("day") === date }.
      groupBy { case(entry, ((version, _), ((behavior, team), _))) => team.id }.
      map { case(teamId, q) =>
        (teamId, q.length)
      }
  }
  val countsForDateQuery = Compiled(uncompiledCountsForDateQuery _)

  def countsForDate(date: DateTime): Future[Seq[(String, Int)]] = {
    val action = countsForDateQuery(date).result
    dataService.run(action)
  }

  def uncompiledUniqueInvokingUserCountsForDateQuery(date: Rep[DateTime]) = {
    allWithVersion.
      filter { case(entry, _) => entry.createdAt.trunc("day") === date }.
      groupBy { case(entry, ((version, _), ((behavior, team), _))) => (team.id, entry.maybeUserIdForContext.getOrElse("<no user>")) }.
      map { case((teamId, userId), q) =>
        (teamId, userId, 1)
      }.
      groupBy { case(teamId, _, _) => teamId }.
      map { case(teamId, q) => (teamId, q.map(_._3).sum.getOrElse(0)) }
  }
  val uniqueInvokingUserCountsForDateQuery = Compiled(uncompiledUniqueInvokingUserCountsForDateQuery _)

  def uniqueInvokingUserCountsForDate(date: DateTime): Future[Seq[(String, Int)]] = {
    val action = uniqueInvokingUserCountsForDateQuery(date).result
    dataService.run(action)
  }

  def uncompiledUniqueInvokedBehaviorCountsForDateQuery(date: Rep[DateTime]) = {
    allWithVersion.
      filter { case(entry, _) => entry.createdAt.trunc("day") === date }.
      groupBy { case(entry, ((version, _), ((behavior, team), _))) => (team.id, behavior.id) }.
      map { case((teamId, behaviorId), q) =>
        (teamId, behaviorId, 1)
      }.
      groupBy { case(teamId, _, _) => teamId }.
      map { case(teamId, q) => (teamId, q.map(_._3).sum.getOrElse(0)) }
  }
  val uniqueInvokedBehaviorCountsForDateQuery = Compiled(uncompiledUniqueInvokedBehaviorCountsForDateQuery _)

  def uniqueInvokedBehaviorCountsForDate(date: DateTime): Future[Seq[(String, Int)]] = {
    val action = uniqueInvokedBehaviorCountsForDateQuery(date).result
    dataService.run(action)
  }

  def uncompiledForTeamForDateQuery(teamId: Rep[String], date: Rep[DateTime]) = {
    allWithVersion.
      filter { case(entry, ((version, user), ((behavior, team), _))) => teamId === team.id}.
      filter { case(entry, _) => entry.createdAt.trunc("day") === date }
  }
  val forTeamForDateQuery = Compiled(uncompiledForTeamForDateQuery _)

  def forTeamForDate(team: Team, date: DateTime): Future[Seq[InvocationLogEntry]] = {
    val action = forTeamForDateQuery(team.id, date).result.map { r =>
      r.map(tuple2Entry)
    }
    dataService.run(action)
  }

  def uncompiledAllForBehaviorVersionQuery(
                                            behaviorVersionId: Rep[String],
                                            from: Rep[DateTime],
                                            to: Rep[DateTime]
                                          ) = {
    allWithVersion.
      filter { case(entry, _) => entry.behaviorVersionId === behaviorVersionId }.
      filter { case(entry, _) => entry.createdAt >= from && entry.createdAt <= to }
  }
  val allForBehaviorVersionQuery = Compiled(uncompiledAllForBehaviorVersionQuery _)

  def allForBehaviorVersion(
                             behaviorVersion: BehaviorVersion,
                             from: DateTime,
                             to: DateTime
                           ): Future[Seq[InvocationLogEntry]] = {
    val action = allForBehaviorVersionQuery(behaviorVersion.id, from, to).result.map { r =>
      r.map(tuple2Entry)
    }
    dataService.run(action)
  }

  def allForBehavior(behavior: Behavior, from: DateTime, to: DateTime): Future[Seq[InvocationLogEntry]] = {
    for {
      versions <- dataService.behaviorVersions.allFor(behavior)
      entries <- Future.sequence(versions.map(ea => allForBehaviorVersion(ea, from, to))).map(_.flatten)
    } yield entries
  }

  def createFor(
                 behaviorVersion: BehaviorVersion,
                 parametersWithValues: Seq[ParameterWithValue],
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
        Json.toJson(parametersWithValues.map { ea =>
          ea.parameter.name -> ea.preparedValue
        }.toMap),
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
        raw.paramValues,
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
