package models.behaviors.behaviorgroup

import models.team.{Team, TeamQueries}
import slick.driver.PostgresDriver.api._

object BehaviorGroupQueries {

  val all = TableQuery[BehaviorGroupsTable]
  val allWithTeam = all.join(TeamQueries.all).on(_.teamId === _.id)

  type TupleType = (RawBehaviorGroup, Team)

  def tuple2Group(tupleType: TupleType): BehaviorGroup = {
    val raw = tupleType._1
    val team = tupleType._2
    BehaviorGroup(raw.id, raw.name, team, raw.createdAt)
  }
}
