package models.bots

import com.github.tototoshi.slick.PostgresJodaSupport._
import models.IDs
import models.bots.behaviorversion.{BehaviorVersion, BehaviorVersionQueries}
import org.joda.time.DateTime
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global


case class InvocationLogEntry(
                             id: String,
                             behaviorVersionId: String,
                             resultType: String,
                             resultText: String,
                             context: String,
                             maybeUserIdForContext: Option[String],
                             runtimeInMilliseconds: Long,
                             createdAt: DateTime
                               )

class InvocationLogEntriesTable(tag: Tag) extends Table[InvocationLogEntry](tag, "invocation_log_entries") {

  def id = column[String]("id", O.PrimaryKey)
  def behaviorVersionId = column[String]("behavior_version_id")
  def resultType = column[String]("result_type")
  def resultText = column[String]("result_text")
  def context = column[String]("context")
  def maybeUserIdForContext = column[Option[String]]("user_id_for_context")
  def runtimeInMilliseconds = column[Long]("runtime_in_milliseconds")
  def createdAt = column[DateTime]("created_at")

  def * = (id, behaviorVersionId, resultType, resultText, context, maybeUserIdForContext, runtimeInMilliseconds, createdAt) <>
    ((InvocationLogEntry.apply _).tupled, InvocationLogEntry.unapply _)
}

object InvocationLogEntryQueries {

  val all = TableQuery[InvocationLogEntriesTable]
  val allWithVersion = all.join(BehaviorVersionQueries.allWithBehavior).on(_.behaviorVersionId === _._1._1.id)

  val truncateDate = SimpleFunction.binary[String, DateTime, DateTime]("date_trunc")

  def countsByDay: DBIO[Seq[(DateTime, String, Int)]] = {
    allWithVersion.
      map { case(entry, ((version, _), (behavior, team))) =>
        (truncateDate("day", entry.createdAt), team.id, 1)
      }.
      groupBy { case(date, teamId, _) => (date, teamId)}.
      map { case((date, teamId), q) =>
        (date, teamId, q.map(_._3).sum.getOrElse(0))
      }.
      result
  }

  def createFor(
                 behaviorVersion: BehaviorVersion,
                 result: BehaviorResult,
                 context: String,
                 maybeUserIdForContext: Option[String],
                 runtimeInMilliseconds: Long
                 ): DBIO[InvocationLogEntry] = {
    val newInstance =
      InvocationLogEntry(
        IDs.next,
        behaviorVersion.id,
        result.resultType.toString,
        result.fullText,
        context,
        maybeUserIdForContext,
        runtimeInMilliseconds,
        DateTime.now
      )

    (all += newInstance).map(_ => newInstance)
  }

}
