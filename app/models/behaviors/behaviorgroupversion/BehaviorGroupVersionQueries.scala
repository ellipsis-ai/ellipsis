package models.behaviors.behaviorgroupversion

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
      raw.maybeGitSHA,
      raw.createdAt
    )
  }

  def uncompiledFindQuery(id: Rep[String]) = {
    allWithUser.filter { case((version, _), _) => version.id === id }
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  def uncompiledAllForQuery(groupId: Rep[String]) = {
    allWithUser.
      filter { case((version, _), _) => version.groupId === groupId }.
      sortBy { case((version, _), _) => version.createdAt.desc }
  }
  val allForQuery = Compiled(uncompiledAllForQuery _)

  private def uncompiledCurrentIdForQuery(groupId: Rep[String]) = {
    all.filter(_.groupId === groupId).sortBy(_.createdAt.desc).take(1).map(_.id)
  }
  val currentIdForQuery = Compiled(uncompiledCurrentIdForQuery _)

  def uncompiledAllCurrentQuery = {
    // distinctOn() is broken in Slick as of v3.2.1, so we use a subquery
    allWithUser.filter { case((outerVersion, _), _) =>
      !all.filter { innerVersion =>
        innerVersion.groupId === outerVersion.groupId && innerVersion.createdAt > outerVersion.createdAt
      }.exists
    }
  }
  val allCurrentQuery = Compiled(uncompiledAllCurrentQuery)

  private def uncompiledAllCurrentIdsQuery = {
    uncompiledAllCurrentQuery.map { case((version, _), _) => version.id }
  }
  val allCurrentIdsQuery = Compiled(uncompiledAllCurrentIdsQuery)

}
