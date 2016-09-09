package models.accounts.oauth2token

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.OAuth2Info

import scala.concurrent.Future

trait OAuth2TokenService {

  def save(loginInfo: LoginInfo, oauth2Info: OAuth2Info): Future[OAuth2Info]

  def save(token: OAuth2Token): Future[OAuth2Token]

  def deleteToken(token: String): Future[Int]

  def deleteLoginInfo(loginInfo: LoginInfo): Future[Unit]

  def findByLoginInfo(loginInfo: LoginInfo): Future[Option[OAuth2Token]]

  def allFullForSlackTeamId(teamId: String): Future[Seq[OAuth2Token]]

  def maybeFullForSlackTeamId(teamId: String): Future[Option[OAuth2Token]]

}
