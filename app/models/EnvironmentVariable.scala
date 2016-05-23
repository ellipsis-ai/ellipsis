package models

import com.github.tototoshi.slick.PostgresJodaSupport._
import org.joda.time.DateTime
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global

case class EnvironmentVariable(
                              name: String,
                              value: String,
                              team: Team,
                              createdAt: DateTime
                            ) {
  def toRaw: RawEnvironmentVariable = {
    RawEnvironmentVariable(name, value, team.id, createdAt)
  }
}

case class RawEnvironmentVariable(
                                 name: String,
                                 value: String,
                                 teamId: String,
                                 createdAt: DateTime
                                   )

class EnvironmentVariablesTable(tag: Tag) extends Table[RawEnvironmentVariable](tag, "environment_variables") {

  def name = column[String]("name")
  def value = column[String]("value")
  def teamId = column[String]("team_id")
  def createdAt = column[DateTime]("created_at")

  def * = (name, value, teamId, createdAt) <> ((RawEnvironmentVariable.apply _).tupled, RawEnvironmentVariable.unapply _)
}

object EnvironmentVariableQueries {

  val all = TableQuery[EnvironmentVariablesTable]
  val allWithTeam = all.join(Team.all).on(_.teamId === _.id)

  def tuple2EnironmentVariable(tuple: (RawEnvironmentVariable, Team)): EnvironmentVariable = {
    val raw = tuple._1
    EnvironmentVariable(raw.name, raw.value, tuple._2, raw.createdAt)
  }

  def uncompiledFindQueryFor(name: Rep[String], teamId: Rep[String]) = {
    all.filter(_.name === name).filter(_.teamId === teamId)
  }
  val findQueryFor = Compiled(uncompiledFindQueryFor _)

  def ensureFor(name: String, value: String, team: Team): DBIO[EnvironmentVariable] = {
    val newInstance = EnvironmentVariable(name, value, team, DateTime.now)
    val raw = newInstance.toRaw
    val query = findQueryFor(name, team.id)
    query.result.flatMap { r =>
      r.headOption.map { existing =>
        query.update(raw)
      }.getOrElse {
        all += raw
      }
    }.map(_ => newInstance)
  }

  def uncompiledAllForTeamQuery(teamId: Rep[String]) = allWithTeam.filter(_._1.teamId === teamId)
  val allForTeamQuery = Compiled(uncompiledAllForTeamQuery _)

  def allFor(team: Team): DBIO[Seq[EnvironmentVariable]] = {
    allForTeamQuery(team.id).result.map { r => r.map(tuple2EnironmentVariable)}
  }
}
