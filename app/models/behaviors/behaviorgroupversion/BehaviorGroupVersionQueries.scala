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

}
