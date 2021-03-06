package models.behaviors.invocationlogentry

import java.time.OffsetDateTime

import drivers.SlickPostgresDriver.api._
import models.accounts.user.{User, UserQueries}
import models.behaviors.behaviorversion.BehaviorVersionQueries
import models.behaviors.events.EventType
import play.api.libs.json.JsNull

object InvocationLogEntryQueries {

  val all = TableQuery[InvocationLogEntriesTable]
  val allWithUser = all.join(UserQueries.all).on(_.userId === _.id)
  val allWithVersion = allWithUser.join(BehaviorVersionQueries.allWithGroupVersion).on(_._1.behaviorVersionId === _._1._1.id)

  type TupleType = ((RawInvocationLogEntry, User), BehaviorVersionQueries.TupleType)

  def tuple2Entry(tuple: TupleType): InvocationLogEntry = {
    val raw = tuple._1._1
    val user = tuple._1._2
    InvocationLogEntry(
      raw.id,
      BehaviorVersionQueries.tuple2BehaviorVersion(tuple._2),
      raw.resultType,
      EventType.maybeFrom(raw.maybeEventType),
      EventType.maybeFrom(raw.maybeOriginalEventType),
      raw.messageText,
      raw.paramValues.getOrElse(JsNull),
      raw.resultText,
      raw.context,
      raw.maybeChannel,
      raw.maybeUserIdForContext,
      user,
      raw.runtimeInMilliseconds,
      raw.maybeMessageListenerId,
      raw.createdAt
    )
  }

  def uncompiledFindQuery(id: Rep[String]) = {
    allWithVersion.filter { case ((entry, _), _) => entry.id === id }
  }

  val findQuery = Compiled(uncompiledFindQuery _)

  def uncompiledCountsForDateQuery(date: Rep[OffsetDateTime]) = {
    allWithVersion.
      filter { case((entry, _), _) => entry.createdAt.trunc("day") === date }.
      groupBy { case(_, (_, ((_, (_, team)), _))) => team.id }.
      map { case(teamId, q) =>
        (teamId, q.length)
      }
  }
  val countsForDateQuery = Compiled(uncompiledCountsForDateQuery _)

  def uncompiledUniqueInvokingUserCountsForDateQuery(date: Rep[OffsetDateTime]) = {
    allWithVersion.
      filter { case((entry, _), _) => entry.createdAt.trunc("day") === date }.
      groupBy { case((entry, _), (_, ((_, (_, team)), _))) => (team.id, entry.maybeUserIdForContext.getOrElse("<no user>")) }.
      map { case((teamId, userId), q) =>
        (teamId, userId, 1)
      }.
      groupBy { case(teamId, _, _) => teamId }.
      map { case(teamId, q) => (teamId, q.map(_._3).sum.getOrElse(0)) }
  }
  val uniqueInvokingUserCountsForDateQuery = Compiled(uncompiledUniqueInvokingUserCountsForDateQuery _)


  def uncompiledUniqueInvokedBehaviorCountsForDateQuery(date: Rep[OffsetDateTime]) = {
    allWithVersion.
      filter { case((entry, _), _) => entry.createdAt.trunc("day") === date }.
      groupBy { case(_, ((_, _), ((behavior, (_, team)), _))) => (team.id, behavior.id) }.
      map { case((teamId, behaviorId), q) =>
        (teamId, behaviorId, 1)
      }.
      groupBy { case(teamId, _, _) => teamId }.
      map { case(teamId, q) => (teamId, q.map(_._3).sum.getOrElse(0)) }
  }
  val uniqueInvokedBehaviorCountsForDateQuery = Compiled(uncompiledUniqueInvokedBehaviorCountsForDateQuery _)

  def uncompiledForTeamForDateQuery(teamId: Rep[String], date: Rep[OffsetDateTime]) = {
    allWithVersion.
      filter { case(_, (_, ((_, (_, team)), _))) => teamId === team.id}.
      filter { case((entry, _), _) => entry.createdAt.trunc("day") === date }
  }
  val forTeamForDateQuery = Compiled(uncompiledForTeamForDateQuery _)

  def uncompiledForTeamSinceDateQuery(teamId: Rep[String], date: Rep[OffsetDateTime]) = {
    allWithVersion.
      filter { case(_, (_, ((_, (_, team)), _))) => teamId === team.id}.
      filter { case((entry, _), _) => entry.createdAt >= date }
  }
  val forTeamSinceDateQuery = Compiled(uncompiledForTeamSinceDateQuery _)

  def uncompiledAllForBehaviorQuery(
                                     behaviorId: Rep[String],
                                     from: Rep[OffsetDateTime],
                                     to: Rep[OffsetDateTime],
                                     maybeUserId: Rep[Option[String]],
                                     maybeOriginalEventType: Rep[Option[String]]
                                   ) = {
    allWithVersion.
      filter { case(_, ((version, _), _)) => version.behaviorId === behaviorId }.
      filter { case((entry, _), _) => entry.createdAt >= from && entry.createdAt <= to }.
      filter { case((entry, _), _) => maybeUserId.isEmpty || entry.userId === maybeUserId }.
      filter { case((entry, _), _) => maybeOriginalEventType.isEmpty || entry.maybeOriginalEventType === maybeOriginalEventType }
  }
  val allForBehaviorQuery = Compiled(uncompiledAllForBehaviorQuery _)

  private def uncompiledLastForGroupQuery(
                                   groupId: Rep[String]
                                 ) = {
    allWithVersion.
      filter { case(_, (_, ((_, (group, _)), _))) => group.id === groupId }.
      sortBy { case((entry, _), _) => entry.createdAt.desc }.
      take(1).
      map { case((entry, _), _) => entry.createdAt }
  }

  val lastForGroupQuery = Compiled(uncompiledLastForGroupQuery _)

  def uncompiledLastInvocationForTeamQuery(teamId: Rep[String]) = {
    allWithVersion.
      filter { case(_, (_, ((_, (_, team)), _))) => teamId === team.id}.
      sortBy { case((entry, _), _) => entry.createdAt.desc }.
      take(1).
      map { case((entry, _), _) => entry.createdAt }
  }

  val lastInvocationForTeamQuery = Compiled(uncompiledLastInvocationForTeamQuery _)

  private def uncompiledLastForEachGroupForTeamQuery(teamId: Rep[String]) = {
    allWithVersion.filter {  case (_, ((_, ((_, team), _)), _)) =>
      team.id === teamId
    }.groupBy { case (_, (_, ((groupVersion, _), _))) =>
      groupVersion.groupId
    }.map { case (groupId, entries) =>
      val maybeMostRecent = entries.map { case ((entry, _), _) => entry.createdAt }.max
      (groupId, maybeMostRecent)
    }
  }

  val lastForEachGroupForTeamQuery = Compiled(uncompiledLastForEachGroupForTeamQuery _)

  def uncompiledAllForMessageListenerQuery(messageListenerId: Rep[String], since: Rep[OffsetDateTime]) = {
    allWithVersion.filter { case((entry, _), _) => entry.maybeMessageListenerId === messageListenerId && entry.createdAt > since }
  }
  val allForMessageListenerQuery = Compiled(uncompiledAllForMessageListenerQuery _)
}
