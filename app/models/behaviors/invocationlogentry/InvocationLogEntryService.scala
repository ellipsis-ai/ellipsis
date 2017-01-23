package models.behaviors.invocationlogentry

import java.time.OffsetDateTime

import models.accounts.user.User
import models.behaviors.behavior.Behavior
import models.behaviors.{BotResult, ParameterWithValue}
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.events.MessageEvent
import models.team.Team

import scala.concurrent.Future

trait InvocationLogEntryService {

  def countsForDate(date: OffsetDateTime): Future[Seq[(String, Int)]]

  def uniqueInvokingUserCountsForDate(date: OffsetDateTime): Future[Seq[(String, Int)]]

  def uniqueInvokedBehaviorCountsForDate(date: OffsetDateTime): Future[Seq[(String, Int)]]

  def forTeamForDate(team: Team, date: OffsetDateTime): Future[Seq[InvocationLogEntry]]

  def allForBehavior(behavior: Behavior, from: OffsetDateTime, to: OffsetDateTime, maybeUserId: Option[String]): Future[Seq[InvocationLogEntry]]

  def createFor(
                 behaviorVersion: BehaviorVersion,
                 parametersWithValues: Seq[ParameterWithValue],
                 result: BotResult,
                 event: MessageEvent,
                 maybeUserIdForContext: Option[String],
                 user: User,
                 runtimeInMilliseconds: Long
               ): Future[InvocationLogEntry]

}
