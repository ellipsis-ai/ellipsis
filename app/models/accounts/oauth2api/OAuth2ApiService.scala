package models.accounts.oauth2api

import models.team.Team

import scala.concurrent.Future

trait OAuth2ApiService {

  def find(id: String): Future[Option[OAuth2Api]]

  def allFor(maybeTeam: Option[Team]): Future[Seq[OAuth2Api]]

  def createFor(
                 name: String,
                 authorizationUrl: String,
                 accessTokenUrl: String,
                 maybeNewApplicationUrl: Option[String],
                 maybeScopeDocumentationUrl: Option[String]
               ): Future[OAuth2Api]

  def save(api: OAuth2Api): Future[OAuth2Api]

}
