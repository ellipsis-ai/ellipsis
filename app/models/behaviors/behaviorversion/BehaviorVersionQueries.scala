package models.behaviors.behaviorversion

import models.accounts.user.{User, UserQueries}
import models.behaviors.behavior.BehaviorQueries
import slick.driver.PostgresDriver.api._

object BehaviorVersionQueries {

  def all = TableQuery[BehaviorVersionsTable]
  def allWithUser = all.joinLeft(UserQueries.all).on(_.maybeAuthorId === _.id)
  def allWithBehavior = allWithUser.join(BehaviorQueries.allWithGroup).on(_._1.behaviorId === _._1._1.id)

  type TupleType = ((RawBehaviorVersion, Option[User]), BehaviorQueries.TupleType)

  def tuple2BehaviorVersion(tuple: TupleType): BehaviorVersion = {
    val raw = tuple._1._1
    BehaviorVersion(
      raw.id,
      BehaviorQueries.tuple2Behavior(tuple._2),
      raw.maybeDescription,
      raw.maybeShortName,
      raw.maybeFunctionBody,
      raw.maybeResponseTemplate,
      raw.forcePrivateResponse,
      tuple._1._2,
      raw.createdAt
    )
  }

}
