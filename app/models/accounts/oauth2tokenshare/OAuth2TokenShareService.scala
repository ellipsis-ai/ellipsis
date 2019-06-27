package models.accounts.oauth2tokenshare

import models.accounts.oauth2application.OAuth2Application
import models.accounts.user.User
import models.team.Team
import slick.dbio.DBIO

import scala.concurrent.Future

trait OAuth2TokenShareService {

  def allForAction(teamId: String): DBIO[Seq[OAuth2TokenShare]]

  def ensureFor(user: User, application: OAuth2Application): Future[OAuth2TokenShare]

  def removeFor(user: User, application: OAuth2Application, maybeTeam: Option[Team]): Future[Unit]

  def findFor(team: Team, application: OAuth2Application): Future[Option[OAuth2TokenShare]]


}
