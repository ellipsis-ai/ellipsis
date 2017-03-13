package models.behaviors.behavior

import models.behaviors.behaviorgroup.BehaviorGroupQueries
import models.team.{Team, TeamQueries, TeamsTable}
import drivers.SlickPostgresDriver.api._
import models.behaviors.behaviorgroupversion.BehaviorGroupVersionQueries

object BehaviorQueries {

  def all = TableQuery[BehaviorsTable]
  def allWithTeam = all.join(TeamQueries.all).on(_.teamId === _.id)
  def allWithGroup = allWithTeam.joinLeft(BehaviorGroupQueries.allWithTeam).on(_._1.groupId === _._1.id)
  def allWithCurrentGroupVersion = allWithGroup.joinLeft(BehaviorGroupVersionQueries.allWithUser).on(_._2.flatMap(_._1.maybeCurrentVersionId) === _._1._1.id)

  type TupleType = ((RawBehavior, Team), Option[BehaviorGroupQueries.TupleType])
  type TableTupleType = ((BehaviorsTable, TeamsTable), Rep[Option[BehaviorGroupQueries.TableTupleType]])

  def tuple2Behavior(tuple: TupleType): Behavior = {
    val raw = tuple._1._1
    val team = tuple._1._2
    val maybeGroup = tuple._2.map(BehaviorGroupQueries.tuple2Group)
    Behavior(
      raw.id,
      team,
      maybeGroup,
      raw.maybeExportId,
      raw.isDataType,
      raw.createdAt
    )
  }

  def uncompiledFindQuery(id: Rep[String]) = {
    allWithGroup.filter { case((behavior, _), _) => behavior.id === id }
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  def uncompiledAllForTeamQuery(teamId: Rep[String]) = {
    allWithGroup.
      filter { case((behavior, team), _) => team.id === teamId }
  }
  val allForTeamQuery = Compiled(uncompiledAllForTeamQuery _)

  def uncompiledAllForGroupQuery(groupId: Rep[String]) = {
    allWithGroup.filter { case((behavior, team), _) => behavior.groupId === groupId }
  }
  val allForGroupQuery = Compiled(uncompiledAllForGroupQuery _)

  def uncompiledFindRawQuery(id: Rep[String]) = all.filter(_.id === id)
  val findRawQueryFor = Compiled(uncompiledFindRawQuery _)

  val SEARCH_QUERY_PARAM = "searchQuery"

}
