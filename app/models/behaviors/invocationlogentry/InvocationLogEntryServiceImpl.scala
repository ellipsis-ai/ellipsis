package models.behaviors.invocationlogentry

import java.time.OffsetDateTime
import javax.inject.Inject

import com.google.inject.Provider
import models.IDs
import models.behaviors.{BotResult, ParameterWithValue}
import models.behaviors.behaviorversion.BehaviorVersion
import models.team.Team
import services.DataService
import drivers.SlickPostgresDriver.api._
import models.accounts.user.User
import models.behaviors.behavior.Behavior
import play.api.libs.json.{JsValue, Json}
import services.slack.MessageEvent

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
                                  maybeUserId: Option[String],
                                  runtimeInMilliseconds: Long,
                                  createdAt: OffsetDateTime
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
  def maybeUserId = column[Option[String]]("user_id")
  def runtimeInMilliseconds = column[Long]("runtime_in_milliseconds")
  def createdAt = column[OffsetDateTime]("created_at")

  def * = (id, behaviorVersionId, resultType, messageText, paramValues, resultText, context, maybeUserIdForContext, maybeUserId, runtimeInMilliseconds, createdAt) <>
    ((RawInvocationLogEntry.apply _).tupled, RawInvocationLogEntry.unapply _)
}

class InvocationLogEntryServiceImpl @Inject() (
                                             dataServiceProvider: Provider[DataService]
                                           ) extends InvocationLogEntryService {

  def dataService = dataServiceProvider.get

  import InvocationLogEntryQueries._

  def countsForDate(date: OffsetDateTime): Future[Seq[(String, Int)]] = {
    val action = countsForDateQuery(date).result
    dataService.run(action)
  }

  def uniqueInvokingUserCountsForDate(date: OffsetDateTime): Future[Seq[(String, Int)]] = {
    val action = uniqueInvokingUserCountsForDateQuery(date).result
    dataService.run(action)
  }


  def uniqueInvokedBehaviorCountsForDate(date: OffsetDateTime): Future[Seq[(String, Int)]] = {
    val action = uniqueInvokedBehaviorCountsForDateQuery(date).result
    dataService.run(action)
  }


  def forTeamForDate(team: Team, date: OffsetDateTime): Future[Seq[InvocationLogEntry]] = {
    val action = forTeamForDateQuery(team.id, date).result.map { r =>
      r.map(tuple2Entry)
    }
    dataService.run(action)
  }

  def allForBehavior(behavior: Behavior, from: OffsetDateTime, to: OffsetDateTime, maybeUserId: Option[String]): Future[Seq[InvocationLogEntry]] = {
    val action = allForBehaviorQuery(behavior.id, from, to, maybeUserId).result.map { r =>
      r.map(tuple2Entry)
    }
    dataService.run(action)
  }

  def createFor(
                 behaviorVersion: BehaviorVersion,
                 parametersWithValues: Seq[ParameterWithValue],
                 result: BotResult,
                 event: MessageEvent,
                 maybeUserIdForContext: Option[String],
                 user: User,
                 runtimeInMilliseconds: Long
               ): Future[InvocationLogEntry] = {
    val raw =
      RawInvocationLogEntry(
        IDs.next,
        behaviorVersion.id,
        result.resultType.toString,
        event.fullMessageText,
        Json.toJson(parametersWithValues.map { ea =>
          ea.parameter.name -> ea.preparedValue
        }.toMap),
        result.fullText,
        event.name,
        maybeUserIdForContext,
        Some(user.id),
        runtimeInMilliseconds,
        OffsetDateTime.now
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
        Some(user),
        raw.runtimeInMilliseconds,
        raw.createdAt
      )
    }
    dataService.run(action)
  }
}
