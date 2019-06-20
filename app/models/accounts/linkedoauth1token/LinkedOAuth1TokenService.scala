package models.accounts.linkedoauth1token

import models.accounts.oauth2application.OAuth2Application
import models.accounts.user.User
import play.api.libs.ws.WSClient
import slick.dbio.DBIO

import scala.concurrent.Future

trait LinkedOAuth1TokenService {

  def sharedForUserAction(user: User, ws: WSClient): DBIO[Seq[LinkedOAuth1Token]]

  def allForUserAction(user: User, ws: WSClient): DBIO[Seq[LinkedOAuth1Token]]

  def allForUser(user: User, ws: WSClient): Future[Seq[LinkedOAuth1Token]]

  def save(token: LinkedOAuth1Token): Future[LinkedOAuth1Token]

  def deleteFor(application: OAuth2Application, user: User): Future[Boolean]

}
