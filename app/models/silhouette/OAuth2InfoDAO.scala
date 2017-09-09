package models.silhouette

import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.OAuth2Info
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

class OAuth2InfoDAO @Inject() (
                                dataService: DataService,
                                implicit val ec: ExecutionContext
                              ) extends DelegableAuthInfoDAO[OAuth2Info] {

  def find(loginInfo: LoginInfo): Future[Option[OAuth2Info]] = {
    dataService.oauth2Tokens.findByLoginInfo(loginInfo).map(_.map(_.oauth2Info))
  }

  def add(loginInfo: LoginInfo, authInfo: OAuth2Info): Future[OAuth2Info] = {
    dataService.oauth2Tokens.save(loginInfo, authInfo)
  }

  def update(loginInfo: LoginInfo, authInfo: OAuth2Info): Future[OAuth2Info] = {
    dataService.oauth2Tokens.save(loginInfo, authInfo)
  }

  def save(loginInfo: LoginInfo, authInfo: OAuth2Info): Future[OAuth2Info] = {
    dataService.oauth2Tokens.save(loginInfo, authInfo)
  }

  def remove(loginInfo: LoginInfo): Future[Unit] = {
    dataService.oauth2Tokens.deleteLoginInfo(loginInfo)
  }

}
