package models.behaviors.invocationlogentry

import java.time.OffsetDateTime

import drivers.SlickPostgresDriver.api._
import models.accounts.user.{User, UserQueries}
import models.behaviors.behaviorversion.BehaviorVersionQueries

object InvocationLogEntryQueries {

  val all = TableQuery[InvocationLogEntriesTable]
  val allWithUser = all.joinLeft(UserQueries.all).on(_.maybeUserId === _.id)
  val allWithVersion = allWithUser.join(BehaviorVersionQueries.allWithGroupVersion).on(_._1.behaviorVersionId === _._1._1._1.id)

  type TupleType = ((RawInvocationLogEntry, Option[User]), BehaviorVersionQueries.TupleType)

  def tuple2Entry(tuple: TupleType): InvocationLogEntry = {
    val raw = tuple._1._1
    val maybeUser = tuple._1._2
    InvocationLogEntry(
      raw.id,
      BehaviorVersionQueries.tuple2BehaviorVersion(tuple._2),
      raw.resultType,
      raw.messageText,
      raw.paramValues,
      raw.resultText,
      raw.context,
      raw.maybeUserIdForContext,
      maybeUser,
      raw.runtimeInMilliseconds,
      raw.createdAt
    )
  }

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

  def uncompiledAllForBehaviorQuery(
                                     behaviorId: Rep[String],
                                     from: Rep[OffsetDateTime],
                                     to: Rep[OffsetDateTime],
                                     maybeUserId: Rep[Option[String]]
                                   ) = {
    allWithVersion.
      filter { case(_, (((version, _), _), _)) => version.behaviorId === behaviorId }.
      filter { case((entry, _), _) => entry.createdAt >= from && entry.createdAt <= to }.
      filter { case((entry, _), _) => maybeUserId.isEmpty || entry.maybeUserId === maybeUserId }
  }
  val allForBehaviorQuery = Compiled(uncompiledAllForBehaviorQuery _)
}
