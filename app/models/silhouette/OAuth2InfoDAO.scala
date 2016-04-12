package models.silhouette

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.daos.DelegableAuthInfoDAO
import com.mohiva.play.silhouette.impl.providers.OAuth2Info
import models.{Models, OAuth2Token}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class OAuth2InfoDAO extends DelegableAuthInfoDAO[OAuth2Info] {
  def find(loginInfo: LoginInfo): Future[Option[OAuth2Info]] = Models.run(OAuth2Token.findByLoginInfo(loginInfo)).map(_.map(_.oauth2Info))
  def add(loginInfo: LoginInfo, authInfo: OAuth2Info): Future[OAuth2Info] = Models.run(OAuth2Token.save(loginInfo, authInfo))
  def update(loginInfo: LoginInfo, authInfo: OAuth2Info): Future[OAuth2Info] = Models.run(OAuth2Token.save(loginInfo, authInfo))
  def save(loginInfo: LoginInfo, authInfo: OAuth2Info): Future[OAuth2Info] = Models.run(OAuth2Token.save(loginInfo, authInfo))
  def remove(loginInfo: LoginInfo): Future[Unit] = Models.run(OAuth2Token.deleteLoginInfo(loginInfo))
}
