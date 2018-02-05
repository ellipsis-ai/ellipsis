package models.team_stats


import java.time.OffsetDateTime

import models.team.Team

import scala.concurrent.Future


trait TeamStatService {

  def allStats: Future[Seq[TeamStat]]

  def activeUsersCountFor(team: Team, start: OffsetDateTime, end: OffsetDateTime): Future[Int]

}
