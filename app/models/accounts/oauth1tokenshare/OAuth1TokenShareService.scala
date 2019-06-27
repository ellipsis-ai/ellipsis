package models.accounts.oauth1tokenshare

import models.accounts.oauth1application.OAuth1Application
import models.accounts.user.User
import models.team.Team
import slick.dbio.DBIO

import scala.concurrent.Future

trait OAuth1TokenShareService {

  def allForAction(teamId: String): DBIO[Seq[OAuth1TokenShare]]

  def ensureFor(user: User, application: OAuth1Application): Future[OAuth1TokenShare]

  def findFor(team: Team, application: OAuth1Application): Future[Option[OAuth1TokenShare]]

}
