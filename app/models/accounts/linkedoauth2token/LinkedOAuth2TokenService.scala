package models.accounts.linkedoauth2token

import models.accounts.oauth2application.OAuth2Application
import models.accounts.user.User
import play.api.libs.ws.WSClient
import slick.dbio.DBIO

import scala.concurrent.Future

trait LinkedOAuth2TokenService {

  def allForUserAction(user: User, ws: WSClient): DBIO[Seq[LinkedOAuth2Token]]

  def allForUser(user: User, ws: WSClient): Future[Seq[LinkedOAuth2Token]]

  def save(token: LinkedOAuth2Token): Future[LinkedOAuth2Token]

  def deleteFor(application: OAuth2Application, user: User): Future[Boolean]

}
