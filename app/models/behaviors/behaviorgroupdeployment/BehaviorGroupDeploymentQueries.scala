package models.behaviors.behaviorgroupdeployment

import drivers.SlickPostgresDriver.api._
import models.accounts.user.UserQueries

object BehaviorGroupDeploymentQueries {

  val all = TableQuery[BehaviorGroupDeploymentsTable]
  val allWithUser = all.join(UserQueries.all).on(_.userId === _.id)

  private def uncompiledAllForTeamQuery(teamId: Rep[String]) = {
    allWithUser.
      filter { case(_, user) => user.teamId === teamId }.
      map { case(dep, _) => dep }
  }
  val allForTeamQuery = Compiled(uncompiledAllForTeamQuery _)

  private def uncompiledAllForBehaviorGroupQuery(groupId: Rep[String]) = {
    all.filter(_.groupId === groupId)
  }
  val allForBehaviorGroupQuery = Compiled(uncompiledAllForBehaviorGroupQuery _)

  private def uncompiledMostRecentForBehaviorGroupQuery(groupId: Rep[String]) = {
    uncompiledAllForBehaviorGroupQuery(groupId).sortBy(_.createdAt.desc).take(1)
  }
  val mostRecentForBehaviorGroupQuery = Compiled(uncompiledMostRecentForBehaviorGroupQuery _)

  private def uncompiledFindForBehaviorGroupVersionQuery(groupVersionId: Rep[String]) = {
    all.filter(_.groupVersionId === groupVersionId)
  }
  val findForBehaviorGroupVersionQuery = Compiled(uncompiledFindForBehaviorGroupVersionQuery _)

}
