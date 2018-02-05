package services.stats

import java.time.OffsetDateTime

import models.organization.Organization

import scala.concurrent.Future


trait StatsService {

  def activeUsersFor(organization: Organization, start: OffsetDateTime, end: OffsetDateTime): Future[Int]

}
