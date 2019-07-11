package models.behaviors.behaviorgroupversion

import java.time.OffsetDateTime

import drivers.SlickPostgresDriver.api._
import models.accounts.user.{User, UserQueries, UsersTable}
import models.behaviors.behaviorgroup.BehaviorGroupQueries

object BehaviorGroupVersionQueries {

  def all = TableQuery[BehaviorGroupVersionsTable]
  def allWithGroup = all.join(BehaviorGroupQueries.allWithTeam).on(_.groupId === _._1.id)
  def allWithUser = allWithGroup.joinLeft(UserQueries.all).on(_._1.maybeAuthorId === _.id)

  type TupleType = ((RawBehaviorGroupVersion, BehaviorGroupQueries.TupleType), Option[User])
  type TableTupleType = ((BehaviorGroupVersionsTable, BehaviorGroupQueries.TableTupleType), Option[UsersTable])

  def tuple2BehaviorGroupVersion(tuple: TupleType): BehaviorGroupVersion = {
    val raw = tuple._1._1
    val group = BehaviorGroupQueries.tuple2Group(tuple._1._2)
    val maybeAuthor = tuple._2
    BehaviorGroupVersion(
      raw.id,
      group,
      raw.name,
      raw.maybeIcon,
      raw.maybeDescription,
      maybeAuthor,
      raw.createdAt
    )
  }

  def uncompiledFindQuery(id: Rep[String]) = {
    allWithUser.filter { case((version, _), _) => version.id === id }
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  def uncompiledBatchForQuery(groupId: Rep[String], batchSize: ConstColumn[Long], offset: ConstColumn[Long]) = {
    allWithUser.
      filter { case((version, _), _) => version.groupId === groupId }.
      sortBy { case((version, _), _) => version.createdAt.desc }.
      drop(offset).
      take(batchSize)

  }
  val batchForQuery = Compiled(uncompiledBatchForQuery _)

  private def uncompiledCurrentIdForQuery(groupId: Rep[String]) = {
    all.filter(_.groupId === groupId).sortBy(_.createdAt.desc).take(1).map(_.id)
  }
  val currentIdForQuery = Compiled(uncompiledCurrentIdForQuery _)

  private def uncompiledFirstIdForQuery(groupId: Rep[String]) = {
    all.filter(_.groupId === groupId).sortBy(_.createdAt.asc).take(1).map(_.id)
  }
  val firstIdForQuery = Compiled(uncompiledFirstIdForQuery _)

  def uncompiledAllCurrentQuery = {
    // distinctOn() is broken in Slick as of v3.2.1, so we use a subquery
    allWithUser.filter { case((outerVersion, _), _) =>
      !all.filter { innerVersion =>
        innerVersion.groupId === outerVersion.groupId && innerVersion.createdAt > outerVersion.createdAt
      }.exists
    }
  }
  val allCurrentQuery = Compiled(uncompiledAllCurrentQuery)

  def uncompiledAllCurrentForTeam(teamId: Rep[String]) = {
    allWithUser.filter { case((outerVersion, (_, team)), _) =>
      team.id === teamId &&
      !all.filter { innerVersion =>
        innerVersion.groupId === outerVersion.groupId && innerVersion.createdAt > outerVersion.createdAt
      }.exists
    }
  }
  val allCurrentForTeam = Compiled(uncompiledAllCurrentForTeam _)

  def uncompiledAllFirstForTeam(teamId: Rep[String]) = {
    allWithUser.filter { case((outerVersion, (_, team)), _) =>
      team.id === teamId &&
        !all.filter { innerVersion =>
          innerVersion.groupId === outerVersion.groupId && innerVersion.createdAt < outerVersion.createdAt
        }.exists
    }
  }
  val allFirstForTeam = Compiled(uncompiledAllFirstForTeam _)

  private def uncompiledAllCurrentIdsQuery = {
    uncompiledAllCurrentQuery.map { case((version, _), _) => version.id }
  }
  val allCurrentIdsQuery = Compiled(uncompiledAllCurrentIdsQuery)

  private def uncompiledNewerVersionsForAuthorQuery(groupId: Rep[String], createdAt: Rep[OffsetDateTime], userId: Rep[String]) = {
    all.
      filter(_.groupId === groupId).
      filter(_.createdAt > createdAt).
      filter(_.maybeAuthorId === userId)
  }
  val newerVersionsForAuthorQuery = Compiled(uncompiledNewerVersionsForAuthorQuery _)

}
