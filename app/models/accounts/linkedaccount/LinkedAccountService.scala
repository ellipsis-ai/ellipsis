package models.accounts.linkedaccount

import com.mohiva.play.silhouette.api.LoginInfo
import models.accounts.user.User
import slick.dbio.DBIO

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait LinkedAccountService {

  def find(loginInfo: LoginInfo, teamId: String): Future[Option[LinkedAccount]]

  def save(link: LinkedAccount): Future[LinkedAccount]

  def allFor(user: User): Future[Seq[LinkedAccount]]

  def maybeForSlackForAction(user: User): DBIO[Option[LinkedAccount]]

  def maybeForSlackFor(user: User): Future[Option[LinkedAccount]]

  def maybeSlackUserIdFor(user: User): Future[Option[String]] = {
    maybeForSlackFor(user).map { maybeLinkedAccount =>
      maybeLinkedAccount.map(_.loginInfo.providerKey)
    }
  }

  def isAdminAction(linkedAccount: LinkedAccount): DBIO[Boolean]

  def isAdmin(linkedAccount: LinkedAccount): Future[Boolean]

}
