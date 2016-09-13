package models.bots.behavior

import models.team.{Team, TeamQueries}
import slick.driver.PostgresDriver.api._

object BehaviorQueries {

  def all = TableQuery[BehaviorsTable]
  def allWithTeam = all.join(TeamQueries.all).on(_.teamId === _.id)

  def tuple2Behavior(tuple: (RawBehavior, Team)): Behavior = {
    val raw = tuple._1
    Behavior(
      raw.id,
      tuple._2,
      raw.maybeCurrentVersionId,
      raw.maybeImportedId,
      raw.createdAt
    )
  }

  def uncompiledFindQuery(id: Rep[String]) = {
    allWithTeam.filter { case(behavior, team) => behavior.id === id }
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  def uncompiledAllForTeamQuery(teamId: Rep[String]) = {
    allWithTeam.
      filter { case(behavior, team) => team.id === teamId }
  }
  val allForTeamQuery = Compiled(uncompiledAllForTeamQuery _)

  def uncompiledFindQueryFor(id: Rep[String]) = all.filter(_.id === id)
  val findQueryFor = Compiled(uncompiledFindQueryFor _)

}
