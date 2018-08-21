package models.accounts.oauth1token

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.OAuth1Info

import scala.concurrent.Future

trait OAuth1TokenService {

  def save(loginInfo: LoginInfo, oauth1Info: OAuth1Info): Future[OAuth1Info]

  def save(token: OAuth1Token): Future[OAuth1Token]

  def deleteToken(token: String): Future[Int]

  def deleteLoginInfo(loginInfo: LoginInfo): Future[Unit]

  def findByLoginInfo(loginInfo: LoginInfo): Future[Option[OAuth1Token]]

}
