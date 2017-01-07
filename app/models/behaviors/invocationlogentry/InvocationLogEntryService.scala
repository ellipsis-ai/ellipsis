package models.behaviors.invocationlogentry

import java.time.ZonedDateTime

import models.behaviors.behavior.Behavior
import models.behaviors.{BotResult, ParameterWithValue}
import models.behaviors.behaviorversion.BehaviorVersion
import models.team.Team
import services.slack.MessageEvent

import scala.concurrent.Future

trait InvocationLogEntryService {

  def countsForDate(date: ZonedDateTime): Future[Seq[(String, Int)]]

  def uniqueInvokingUserCountsForDate(date: ZonedDateTime): Future[Seq[(String, Int)]]

  def uniqueInvokedBehaviorCountsForDate(date: ZonedDateTime): Future[Seq[(String, Int)]]

  def forTeamForDate(team: Team, date: ZonedDateTime): Future[Seq[InvocationLogEntry]]

  def allForBehavior(behavior: Behavior, from: ZonedDateTime, to: ZonedDateTime): Future[Seq[InvocationLogEntry]]

  def createFor(
                 behaviorVersion: BehaviorVersion,
                 parametersWithValues: Seq[ParameterWithValue],
                 result: BotResult,
                 event: MessageEvent,
                 maybeUserIdForContext: Option[String],
                 runtimeInMilliseconds: Long
               ): Future[InvocationLogEntry]

}
