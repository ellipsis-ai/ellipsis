package models.accounts.logintoken

import models.accounts.user.User

import scala.concurrent.Future

trait LoginTokenService {
  def find(value: String): Future[Option[LoginToken]]
  def createFor(user: User): Future[LoginToken]
}
