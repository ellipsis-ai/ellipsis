package services.stats

import java.time.OffsetDateTime

import models.organization.Organization

import scala.concurrent.Future


trait StatsService {

  def activeUsersCountFor(organization: Organization, start: OffsetDateTime, end: OffsetDateTime): Future[Int]

}
