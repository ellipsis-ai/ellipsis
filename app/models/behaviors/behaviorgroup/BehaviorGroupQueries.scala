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
    BehaviorGroup(raw.id, raw.name, raw.maybeImportedId, team, raw.createdAt)
  }

  def uncompiledAllForTeamQuery(teamId: Rep[String]) = {
    allWithTeam.filter { case(group, team) => team.id === teamId }
  }
  val allForTeamQuery = Compiled(uncompiledAllForTeamQuery _)

  def uncompiledFindQuery(id: Rep[String]) = {
    allWithTeam.filter { case(group, team) => group.id === id }
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  def uncompiledRawFindQuery(id: Rep[String]) = {
    all.filter(_.id === id)
  }
  val rawFindQuery = Compiled(uncompiledRawFindQuery _)
}
