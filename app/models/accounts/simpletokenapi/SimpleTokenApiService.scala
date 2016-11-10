package models.accounts.simpletokenapi

import models.team.Team

import scala.concurrent.Future

trait SimpleTokenApiService {

  def find(id: String): Future[Option[SimpleTokenApi]]

  def allFor(maybeTeam: Option[Team]): Future[Seq[SimpleTokenApi]]

  def save(api: SimpleTokenApi): Future[SimpleTokenApi]

}
