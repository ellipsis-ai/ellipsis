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

  def activeUsersFor(organization: Organization, start: OffsetDateTime, end: OffsetDateTime): Future[Int] = {
    for {
      teams <- dataService.teams.allTeamsFor(organization)
    } yield {
      10
    }
  }

}
