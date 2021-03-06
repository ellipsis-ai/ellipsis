package models.billing.active_user_record


import java.time.OffsetDateTime

import models.accounts.user.User
import models.organization.Organization

import scala.concurrent.Future


trait ActiveUserRecordService {

  def allRecords: Future[Seq[ActiveUserRecord]]

  def countFor(teamId: String, start: OffsetDateTime, end: OffsetDateTime): Future[Int]

  def create(user: User, when: OffsetDateTime = OffsetDateTime.now): Future[ActiveUserRecord]
}
