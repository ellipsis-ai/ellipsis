package models.team_stats


import scala.concurrent.Future


trait TeamStatService {

  def allStats: Future[Seq[TeamStat]]

}
