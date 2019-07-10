package models.behaviors.behaviorgroupdeployment

import drivers.SlickPostgresDriver.api._
import models.accounts.user.UserQueries
import models.behaviors.behaviorgroup.BehaviorGroupQueries

object BehaviorGroupDeploymentQueries {

  val all = TableQuery[BehaviorGroupDeploymentsTable]
  val allWithUser = all.join(UserQueries.all).on(_.userId === _.id)

  private def uncompiledAllForBehaviorGroupQuery(groupId: Rep[String]) = {
    all.filter(_.groupId === groupId)
  }
  val allForBehaviorGroupQuery = Compiled(uncompiledAllForBehaviorGroupQuery _)

  private def uncompiledFirstForBehaviorGroupQuery(groupId: Rep[String]) = {
    uncompiledAllForBehaviorGroupQuery(groupId).sortBy(_.createdAt.asc).take(1)
  }
  val firstForBehaviorGroupQuery = Compiled(uncompiledMostRecentForBehaviorGroupQuery _)

  private def uncompiledMostRecentForBehaviorGroupQuery(groupId: Rep[String]) = {
    uncompiledAllForBehaviorGroupQuery(groupId).sortBy(_.createdAt.desc).take(1)
  }
  val mostRecentForBehaviorGroupQuery = Compiled(uncompiledMostRecentForBehaviorGroupQuery _)

  private def uncompiledFindForBehaviorGroupVersionQuery(groupVersionId: Rep[String]) = {
    all.filter(_.groupVersionId === groupVersionId)
  }
  val findForBehaviorGroupVersionQuery = Compiled(uncompiledFindForBehaviorGroupVersionQuery _)

  private def uncompiledAllMostRecentQuery() = {
    // distinctOn() is broken in Slick as of v3.2.1, so we use a subquery
    all.filter { outer =>
      !all.filter { inner =>
        inner.groupId === outer.groupId && inner.createdAt > outer.createdAt
      }.exists
    }
  }

  private def uncompiledMostRecentBehaviorGroupVersionIdsQuery() = {
    uncompiledAllMostRecentQuery().map(_.groupVersionId)
  }
  val mostRecentBehaviorGroupVersionIdsQuery = Compiled(uncompiledMostRecentBehaviorGroupVersionIdsQuery)

  private def uncompiledMostRecentForTeamQuery(teamId: Rep[String]) = {
    uncompiledAllMostRecentQuery.join(BehaviorGroupQueries.all).on(_.groupId === _.id).
      filter { case(_, group) => group.teamId === teamId }.
      map { case(dep, _) => dep }
  }
  val mostRecentForTeamQuery = Compiled(uncompiledMostRecentForTeamQuery _)

}
