package models.accounts.oauth1application

import models.team.Team

import scala.concurrent.Future

trait OAuth1ApplicationService {

  def find(id: String): Future[Option[OAuth1Application]]

  def allEditableFor(team: Team): Future[Seq[OAuth1Application]]

  def allUsableFor(team: Team): Future[Seq[OAuth1Application]]

  def save(application: OAuth1Application): Future[OAuth1Application]

}
