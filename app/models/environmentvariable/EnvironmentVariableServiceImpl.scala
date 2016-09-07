package models.environmentvariable

import javax.inject.Inject

import com.github.tototoshi.slick.PostgresJodaSupport._
import com.google.inject.Provider
import models.team._
import org.joda.time.DateTime
import services.DataService
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

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

class EnvironmentVariableServiceImpl @Inject() (
                                      dataServiceProvider: Provider[DataService]
                                    ) extends EnvironmentVariableService {

  def dataService = dataServiceProvider.get

  val all = TableQuery[EnvironmentVariablesTable]
  val allWithTeam = all.join(TeamQueries.all).on(_.teamId === _.id)

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

  def uncompiledRawFindQueryFor(name: Rep[String], teamId: Rep[String]) = {
    all.filter(_.name === name).filter(_.teamId === teamId)
  }
  val rawFindQueryFor = Compiled(uncompiledRawFindQueryFor _)

  def find(name: String, team: Team): Future[Option[EnvironmentVariable]] = {
    dataService.run(findQueryFor(name, team.id).result.map { r => r.headOption.map(tuple2EnvironmentVariable) })
  }

  def ensureFor(name: String, maybeValue: Option[String], team: Team): Future[Option[EnvironmentVariable]] = {
    val action = Option(name).filter(_.trim.nonEmpty).map { nonEmptyName =>
      val query = rawFindQueryFor(name, team.id)
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
    dataService.run(action)
  }

  def deleteFor(name: String, team: Team): Future[Boolean] = {
    val action = rawFindQueryFor(name, team.id).delete.map( result => result > 0)
    dataService.run(action)
  }

  def uncompiledAllForTeamQuery(teamId: Rep[String]) = allWithTeam.filter(_._1.teamId === teamId)
  val allForTeamQuery = Compiled(uncompiledAllForTeamQuery _)

  def allFor(team: Team): Future[Seq[EnvironmentVariable]] = {
    val action = allForTeamQuery(team.id).result.map { r => r.map(tuple2EnvironmentVariable)}
    dataService.run(action)
  }
}
