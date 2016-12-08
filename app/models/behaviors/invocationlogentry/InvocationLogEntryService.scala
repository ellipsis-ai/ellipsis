package models.behaviors.invocationlogentry

import models.behaviors.BotResult
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

  def createFor(
                 behaviorVersion: BehaviorVersion,
                 result: BotResult,
                 event: MessageEvent,
                 maybeUserIdForContext: Option[String],
                 runtimeInMilliseconds: Long
               ): Future[InvocationLogEntry]

}
