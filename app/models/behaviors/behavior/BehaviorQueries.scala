package models.behaviors.behavior

import models.behaviors.behaviorgroup.BehaviorGroupQueries
import models.team.{Team, TeamQueries}
import slick.driver.PostgresDriver.api._

object BehaviorQueries {

  def all = TableQuery[BehaviorsTable]
  def allWithTeam = all.join(TeamQueries.all).on(_.teamId === _.id)
  def allWithGroup = allWithTeam.joinLeft(BehaviorGroupQueries.allWithTeam).on(_._1.groupId === _._1.id)

  type TupleType = ((RawBehavior, Team), Option[BehaviorGroupQueries.TupleType])

  def tuple2Behavior(tuple: TupleType): Behavior = {
    val raw = tuple._1._1
    val team = tuple._1._2
    val maybeGroup = tuple._2.map(BehaviorGroupQueries.tuple2Group)
    Behavior(
      raw.id,
      team,
      maybeGroup,
      raw.maybeCurrentVersionId,
      raw.maybeImportedId,
      raw.maybeDataTypeName,
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

  def uncompiledFindRawQuery(id: Rep[String]) = all.filter(_.id === id)
  val findRawQueryFor = Compiled(uncompiledFindRawQuery _)

  def uncompiledFindWithImportedIdQuery(id: Rep[String], teamId: Rep[String]) = {
    allWithGroup.filter { case((behavior, team), _) => behavior.maybeImportedId === id && team.id === teamId}
  }
  val findWithImportedIdQuery = Compiled(uncompiledFindWithImportedIdQuery _)

  val SEARCH_QUERY_PARAM = "searchQuery"

}
