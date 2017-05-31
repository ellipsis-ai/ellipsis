package models.accounts.oauth2application

import models.team.Team

import scala.concurrent.Future

trait OAuth2ApplicationService {

  def find(id: String): Future[Option[OAuth2Application]]

  def allEditableFor(team: Team): Future[Seq[OAuth2Application]]

  def allUsableFor(team: Team): Future[Seq[OAuth2Application]]

  def save(application: OAuth2Application): Future[OAuth2Application]

}
