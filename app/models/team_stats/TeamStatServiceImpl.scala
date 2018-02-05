package models.team_stats

import java.time.OffsetDateTime
import javax.inject.Inject

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import models.team.Team
import play.api.Configuration
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

class TeamStatServiceImpl @Inject() (
                                  val dataServiceProvider: Provider[DataService],
                                  configuration: Configuration,
                                  implicit val ec: ExecutionContext
                                ) extends TeamStatService {

  def dataService = dataServiceProvider.get

  import TeamStatQueries._

  def allStats: Future[Seq[TeamStat]] = {
    dataService.run(all.result)
  }

  def activeUsersCountFor(team: Team, start: OffsetDateTime, end: OffsetDateTime): Future[Int] = {
    Future { 10 }
  }
}
