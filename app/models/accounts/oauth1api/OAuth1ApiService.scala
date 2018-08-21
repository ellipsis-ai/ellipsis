package models.accounts.oauth1api

import models.team.Team

import scala.concurrent.Future

trait OAuth1ApiService {

  def find(id: String): Future[Option[OAuth1Api]]

  def allFor(maybeTeam: Option[Team]): Future[Seq[OAuth1Api]]

  def save(api: OAuth1Api): Future[OAuth1Api]

}
