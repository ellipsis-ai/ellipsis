package models.behaviors.invocationlogentry

import models.behaviors.BotResult
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.events.MessageEvent
import models.team.Team
import org.joda.time.DateTime

import scala.concurrent.Future

trait InvocationLogEntryService {

  def countsForDate(date: DateTime): Future[Seq[(String, Int)]]

  def uniqueInvokingUserCountsForDate(date: DateTime): Future[Seq[(String, Int)]]

  def uniqueInvokedBehaviorCountsForDate(date: DateTime): Future[Seq[(String, Int)]]

  def forTeamByDay(team: Team): Future[Seq[(DateTime, Seq[InvocationLogEntry])]]

  def createFor(
                 behaviorVersion: BehaviorVersion,
                 result: BotResult,
                 event: MessageEvent,
                 maybeUserIdForContext: Option[String],
                 runtimeInMilliseconds: Long
               ): Future[InvocationLogEntry]

}
