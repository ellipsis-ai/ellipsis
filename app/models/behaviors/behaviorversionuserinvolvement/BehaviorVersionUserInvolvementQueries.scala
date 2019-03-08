package models.behaviors.behaviorversionuserinvolvement

import java.time.OffsetDateTime

import drivers.SlickPostgresDriver.api._
import models.accounts.user.{User, UserQueries, UsersTable}
import models.behaviors.behaviorversion.BehaviorVersionQueries

object BehaviorVersionUserInvolvementQueries {

  def all = TableQuery[BehaviorVersionUserInvolvementsTable]
  def allWithBehaviorVersion = all.join(BehaviorVersionQueries.allWithGroupVersion).on(_.behaviorVersionId === _._1._1.id)
  def allWithUser = allWithBehaviorVersion.join(UserQueries.all).on(_._1.userId === _.id)

  type TupleType = ((RawBehaviorVersionUserInvolvement, BehaviorVersionQueries.TupleType), User)
  type TableTupleType = ((BehaviorVersionUserInvolvementsTable, BehaviorVersionQueries.TableTupleType), UsersTable)

  def tuple2Involvement(tuple: TupleType): BehaviorVersionUserInvolvement = {
    val raw = tuple._1._1
    val behaviorVersion = BehaviorVersionQueries.tuple2BehaviorVersion(tuple._1._2)
    val user = tuple._2
    BehaviorVersionUserInvolvement(
      raw.id,
      behaviorVersion,
      user,
      raw.createdAt
    )
  }

  def uncompiledFindAllForTeamBetweenQuery(teamId: Rep[String], start: Rep[OffsetDateTime], end: Rep[OffsetDateTime]) = {
    allWithUser.
      filter { case((_, ((_, ((_, team), _)), _)), _) => team.id === teamId }.
      filter { case((involvement, _), _) => involvement.createdAt >= start && involvement.createdAt < end }
  }
  val findAllForTeamBetweenQuery = Compiled(uncompiledFindAllForTeamBetweenQuery _)

}
