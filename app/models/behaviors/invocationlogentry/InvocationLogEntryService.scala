package models.behaviors.invocationlogentry

import java.time.OffsetDateTime

import models.accounts.user.User
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.{BotResult, ParameterWithValue}
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.events.{Event, EventType}
import models.team.Team
import slick.dbio.DBIO

import scala.concurrent.Future

trait InvocationLogEntryService {

  def countsForDate(date: OffsetDateTime): Future[Seq[(String, Int)]]

  def uniqueInvokingUserCountsForDate(date: OffsetDateTime): Future[Seq[(String, Int)]]

  def uniqueInvokedBehaviorCountsForDate(date: OffsetDateTime): Future[Seq[(String, Int)]]

  def forTeamForDate(team: Team, date: OffsetDateTime): Future[Seq[InvocationLogEntry]]

  def forTeamSinceDate(team: Team, date: OffsetDateTime): Future[Seq[InvocationLogEntry]]

  def allForBehavior(behavior: Behavior, from: OffsetDateTime, to: OffsetDateTime, maybeUserId: Option[String], maybeOriginalEventType: Option[EventType]): Future[Seq[InvocationLogEntry]]

  def lastForGroupAction(group: BehaviorGroup): DBIO[Option[OffsetDateTime]]

  def lastForGroup(group: BehaviorGroup): Future[Option[OffsetDateTime]]

  case class BehaviorGroupInvocationTimestamp(groupId: String, maybeTimestamp: Option[OffsetDateTime])

  def lastForEachGroupForTeamAction(team: Team): DBIO[Seq[BehaviorGroupInvocationTimestamp]]

  def createForAction(
                       behaviorVersion: BehaviorVersion,
                       parametersWithValues: Seq[ParameterWithValue],
                       result: BotResult,
                       event: Event,
                       maybeUserIdForContext: Option[String],
                       user: User,
                       runtimeInMilliseconds: Long
                     ): DBIO[InvocationLogEntry]

  def lastInvocationDateForTeamAction(team: Team): DBIO[Option[OffsetDateTime]]
  def lastInvocationDateForTeam(team: Team): Future[Option[OffsetDateTime]]

}
