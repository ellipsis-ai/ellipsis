package models.bots

import models.{IDs, Team}
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global

case class Behavior(id: String, team: Team, description: String) {

  def resultFor(params: Map[String, String]): String = {
    params.getOrElse("0", "Hm?")
  }

}

case class RawBehavior(id: String, teamId: String, description: String)

class BehaviorsTable(tag: Tag) extends Table[RawBehavior](tag, "behaviors") {

  def id = column[String]("id", O.PrimaryKey)
  def teamId = column[String]("team_id")
  def description = column[String]("description")

  def * =
    (id, teamId, description) <> ((RawBehavior.apply _).tupled, RawBehavior.unapply _)
}

object BehaviorQueries {

  def all = TableQuery[BehaviorsTable]
  def allWithTeam = all.join(Team.all).on(_.teamId === _.id)

  def tuple2Behavior(tuple: (RawBehavior, Team)): Behavior = {
    val rawBehavior = tuple._1
    Behavior(rawBehavior.id, tuple._2, rawBehavior.description)
  }

  def uncompiledAllForTeamQuery(teamId: Rep[String]) = {
    allWithTeam.
      filter { case(behavior, team) => team.id === teamId }
  }
  val allForTeamQuery = Compiled(uncompiledAllForTeamQuery _)

  def allForTeam(team: Team): DBIO[Seq[Behavior]] = {
    allForTeamQuery(team.id).result.map { tuples => tuples.map(tuple2Behavior) }
  }

  def createFor(team: Team, description: String): DBIO[Behavior] = {
    val raw = RawBehavior(IDs.next, team.id, description)

    (all += raw).map { _ => Behavior(raw.id, team, description) }
  }
}
