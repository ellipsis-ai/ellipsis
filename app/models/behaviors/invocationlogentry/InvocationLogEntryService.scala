package models.behaviors.invocationlogentry

import models.behaviors.behavior.Behavior
import models.behaviors.{BotResult, ParameterWithValue}
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.events.MessageEvent
import models.team.Team
import org.joda.time.LocalDateTime

import scala.concurrent.Future

trait InvocationLogEntryService {

  def countsForDate(date: LocalDateTime): Future[Seq[(String, Int)]]

  def uniqueInvokingUserCountsForDate(date: LocalDateTime): Future[Seq[(String, Int)]]

  def uniqueInvokedBehaviorCountsForDate(date: LocalDateTime): Future[Seq[(String, Int)]]

  def forTeamForDate(team: Team, date: LocalDateTime): Future[Seq[InvocationLogEntry]]

  def allForBehavior(behavior: Behavior): Future[Seq[InvocationLogEntry]]

  def createFor(
                 behaviorVersion: BehaviorVersion,
                 parametersWithValues: Seq[ParameterWithValue],
                 result: BotResult,
                 event: MessageEvent,
                 maybeUserIdForContext: Option[String],
                 runtimeInMilliseconds: Long
               ): Future[InvocationLogEntry]

}
