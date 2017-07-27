package models.accounts.logintoken

import models.accounts.user.User
import slick.dbio.DBIO

import scala.concurrent.Future

trait LoginTokenService {
  def find(value: String): Future[Option[LoginToken]]
  def createForAction(user: User): DBIO[LoginToken]
}
