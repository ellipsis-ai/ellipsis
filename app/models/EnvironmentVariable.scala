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

  def tuple2EnvironmentVariable(tuple: (RawEnvironmentVariable, Team)): EnvironmentVariable = {
    val raw = tuple._1
    EnvironmentVariable(raw.name, raw.value, tuple._2, raw.createdAt)
  }

  def uncompiledFindQueryFor(name: Rep[String], teamId: Rep[String]) = {
    allWithTeam.
      filter { case(envVar, team) => envVar.name === name }.
      filter { case(envVar, team) => team.id === teamId }
  }
  val findQueryFor = Compiled(uncompiledFindQueryFor _)

  def find(name: String, team: Team): DBIO[Option[EnvironmentVariable]] = {
    findQueryFor(name, team.id).result.map { r => r.headOption.map(tuple2EnvironmentVariable) }
  }

  def ensureFor(name: String, maybeValue: Option[String], team: Team): DBIO[Option[EnvironmentVariable]] = {
    Option(name).filter(_.trim.nonEmpty).map { nonEmptyName =>
      val query = all.filter(_.name === name).filter(_.teamId === team.id)
      query.result.flatMap { r =>
        r.headOption.map { existing =>
          maybeValue.map { value =>
            val raw = RawEnvironmentVariable(name, value, team.id, DateTime.now)
            query.update(raw).map(_ => raw)
          }.getOrElse(DBIO.successful(existing))
        }.getOrElse {
          val value = maybeValue.getOrElse("")
          val raw = RawEnvironmentVariable(name, value, team.id, DateTime.now)
          (all += raw).map(_ => raw)
        }.map { raw =>
          Some(EnvironmentVariable(raw.name, raw.value, team, raw.createdAt))
        }
      }
    }.getOrElse(DBIO.successful(None))
  }

  def uncompiledAllForTeamQuery(teamId: Rep[String]) = allWithTeam.filter(_._1.teamId === teamId)
  val allForTeamQuery = Compiled(uncompiledAllForTeamQuery _)

  def allFor(team: Team): DBIO[Seq[EnvironmentVariable]] = {
    allForTeamQuery(team.id).result.map { r => r.map(tuple2EnvironmentVariable)}
  }
}
