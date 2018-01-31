package models.team_stats

import javax.inject.Inject

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
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

}
