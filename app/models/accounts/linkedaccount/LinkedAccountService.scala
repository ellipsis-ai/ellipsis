package models.accounts.linkedaccount

import com.mohiva.play.silhouette.api.LoginInfo
import models.accounts.user.User

import scala.concurrent.Future

trait LinkedAccountService {

  def find(loginInfo: LoginInfo, teamId: String): Future[Option[LinkedAccount]]

  def save(link: LinkedAccount): Future[LinkedAccount]

  def allFor(user: User): Future[Seq[LinkedAccount]]

  def maybeForSlackFor(user: User): Future[Option[LinkedAccount]]

  def isAdmin(linkedAccount: LinkedAccount): Future[Boolean]

}
