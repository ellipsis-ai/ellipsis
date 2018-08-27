package models.silhouette

import javax.inject.Inject
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.{OAuth1Info, OAuth2Info}
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

class OAuth1InfoDAO @Inject() (
                                dataService: DataService,
                                implicit val ec: ExecutionContext
                              ) extends DelegableAuthInfoDAO[OAuth1Info] {

  def find(loginInfo: LoginInfo): Future[Option[OAuth1Info]] = {
    dataService.oauth1Tokens.findByLoginInfo(loginInfo).map(_.map(_.oauth1Info))
  }

  def add(loginInfo: LoginInfo, authInfo: OAuth1Info): Future[OAuth1Info] = {
    dataService.oauth1Tokens.save(loginInfo, authInfo)
  }

  def update(loginInfo: LoginInfo, authInfo: OAuth1Info): Future[OAuth1Info] = {
    dataService.oauth1Tokens.save(loginInfo, authInfo)
  }

  def save(loginInfo: LoginInfo, authInfo: OAuth1Info): Future[OAuth1Info] = {
    dataService.oauth1Tokens.save(loginInfo, authInfo)
  }

  def remove(loginInfo: LoginInfo): Future[Unit] = {
    dataService.oauth1Tokens.deleteLoginInfo(loginInfo)
  }

}
