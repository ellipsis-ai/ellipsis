package models.behaviors.behaviorversion

import models.accounts.user.{User, UserQueries, UsersTable}
import models.behaviors.behavior.BehaviorQueries
import drivers.SlickPostgresDriver.api._
import models.behaviors.behaviorgroupversion.BehaviorGroupVersionQueries

object BehaviorVersionQueries {

  def all = TableQuery[BehaviorVersionsTable]
  def allWithUser = all.joinLeft(UserQueries.all).on(_.maybeAuthorId === _.id)
  def allWithBehavior = allWithUser.join(BehaviorQueries.allWithGroup).on(_._1.behaviorId === _._1._1.id)
  def allWithGroupVersion = allWithBehavior.join(BehaviorGroupVersionQueries.allWithUser).on(_._1._1.groupVersionId === _._1._1.id)

  type TupleType = (((RawBehaviorVersion, Option[User]), BehaviorQueries.TupleType), BehaviorGroupVersionQueries.TupleType)
  type TableTupleType = (((BehaviorVersionsTable, Rep[Option[UsersTable]]), BehaviorQueries.TableTupleType), BehaviorGroupVersionQueries.TableTupleType)

  def tuple2BehaviorVersion(tuple: TupleType): BehaviorVersion = {
    val raw = tuple._1._1._1
    val groupVersion = BehaviorGroupVersionQueries.tuple2BehaviorGroupVersion(tuple._2)
    BehaviorVersion(
      raw.id,
      BehaviorQueries.tuple2Behavior(tuple._1._2),
      groupVersion,
      raw.maybeDescription,
      raw.maybeName,
      raw.maybeFunctionBody,
      raw.maybeResponseTemplate,
      raw.forcePrivateResponse,
      tuple._1._1._2,
      raw.createdAt
    )
  }

}
