package models.behaviors.invocationlogentry

import models.behaviors.behavior.Behavior
import models.behaviors.{BotResult, ParameterWithValue}
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.events.MessageEvent
import models.team.Team
import org.joda.time.DateTime

import scala.concurrent.Future

trait InvocationLogEntryService {

  def countsForDate(date: DateTime): Future[Seq[(String, Int)]]

  def uniqueInvokingUserCountsForDate(date: DateTime): Future[Seq[(String, Int)]]

  def uniqueInvokedBehaviorCountsForDate(date: DateTime): Future[Seq[(String, Int)]]

  def forTeamForDate(team: Team, date: DateTime): Future[Seq[InvocationLogEntry]]

  def allForBehavior(behavior: Behavior, from: DateTime, to: DateTime): Future[Seq[InvocationLogEntry]]

  def createFor(
                 behaviorVersion: BehaviorVersion,
                 parametersWithValues: Seq[ParameterWithValue],
                 result: BotResult,
                 event: MessageEvent,
                 maybeUserIdForContext: Option[String],
                 runtimeInMilliseconds: Long
               ): Future[InvocationLogEntry]

}
