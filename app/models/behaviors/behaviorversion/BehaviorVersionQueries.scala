package models.behaviors.behaviorversion

import models.accounts.user.{User, UserQueries}
import models.behaviors.behavior.{BehaviorQueries, RawBehavior}
import models.team.Team
import slick.driver.PostgresDriver.api._

object BehaviorVersionQueries {

  def all = TableQuery[BehaviorVersionsTable]
  def allWithUser = all.joinLeft(UserQueries.all).on(_.maybeAuthorId === _.id)
  def allWithBehavior = allWithUser.join(BehaviorQueries.allWithTeam).on(_._1.behaviorId === _._1.id)

  type TupleType = ((RawBehaviorVersion, Option[User]), (RawBehavior, Team))

  def tuple2BehaviorVersion(tuple: TupleType): BehaviorVersion = {
    val raw = tuple._1._1
    BehaviorVersion(
      raw.id,
      BehaviorQueries.tuple2Behavior(tuple._2),
      raw.maybeDescription,
      raw.maybeShortName,
      raw.maybeFunctionBody,
      raw.maybeResponseTemplate,
      tuple._1._2,
      raw.createdAt
    )
  }

}
