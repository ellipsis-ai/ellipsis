package services.stats

import java.time.OffsetDateTime
import javax.inject.Inject

import models.organization.Organization
import play.api.Configuration
import services.DataService

import scala.concurrent.{ExecutionContext, Future}


class StatsServiceImpl @Inject()(
                                  val configuration: Configuration,
                                  val dataService: DataService,
                                  implicit val ec: ExecutionContext
                                ) extends StatsService {

  def activeUsersCountFor(organization: Organization, start: OffsetDateTime, end: OffsetDateTime): Future[Int] = {
    for {
      teams <- dataService.teams.allTeamsFor(organization)
      allCountPerTeam <- Future.sequence(teams.map(team => dataService.activeUserRecords.countFor(team.id, start, end)))
      countPerOrg <- Future.successful(allCountPerTeam.reduce(_+_))
    } yield {
      countPerOrg
    }
  }

}
