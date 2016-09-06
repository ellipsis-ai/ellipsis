package models.silhouette

import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.OAuth2Info
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import models.Models
import models.accounts.OAuth2Token

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class OAuth2InfoDAO @Inject() (models: Models) extends DelegableAuthInfoDAO[OAuth2Info] {
  def find(loginInfo: LoginInfo): Future[Option[OAuth2Info]] = models.run(OAuth2Token.findByLoginInfo(loginInfo)).map(_.map(_.oauth2Info))
  def add(loginInfo: LoginInfo, authInfo: OAuth2Info): Future[OAuth2Info] = models.run(OAuth2Token.save(loginInfo, authInfo))
  def update(loginInfo: LoginInfo, authInfo: OAuth2Info): Future[OAuth2Info] = models.run(OAuth2Token.save(loginInfo, authInfo))
  def save(loginInfo: LoginInfo, authInfo: OAuth2Info): Future[OAuth2Info] = models.run(OAuth2Token.save(loginInfo, authInfo))
  def remove(loginInfo: LoginInfo): Future[Unit] = models.run(OAuth2Token.deleteLoginInfo(loginInfo))
}
