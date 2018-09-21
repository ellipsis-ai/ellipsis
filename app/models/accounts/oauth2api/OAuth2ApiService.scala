package models.accounts.oauth2api

import models.team.Team
import slick.dbio.DBIO

import scala.concurrent.Future

trait OAuth2ApiService {

  def findAction(id: String): DBIO[Option[OAuth2Api]]

  def find(id: String): Future[Option[OAuth2Api]]

  def allFor(maybeTeam: Option[Team]): Future[Seq[OAuth2Api]]

  def save(api: OAuth2Api): Future[OAuth2Api]

}
