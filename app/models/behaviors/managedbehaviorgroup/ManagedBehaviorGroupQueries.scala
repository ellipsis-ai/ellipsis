package models.behaviors.managedbehaviorgroup

import drivers.SlickPostgresDriver.api._
import models.behaviors.behaviorgroup.BehaviorGroupQueries
import models.team.TeamQueries

object ManagedBehaviorGroupQueries {

  val all = TableQuery[ManagedBehaviorGroupsTable]

  def uncompiledFindForQuery(groupId: Rep[String]) = {
    all.filter(_.groupId === groupId)
  }
  val findForQuery = Compiled(uncompiledFindForQuery _)

  def uncompiledAllForTeamQuery(teamId: Rep[String]) = {
    all.join(BehaviorGroupQueries.all).on(_.groupId === _.id).
      filter { case(_, group) => group.teamId === teamId }.
      map { case(managed, _) => managed}
  }
  val allForTeamQuery = Compiled(uncompiledAllForTeamQuery _)

}
