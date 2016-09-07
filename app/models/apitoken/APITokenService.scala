package models.apitoken

import models.accounts.user.User

import scala.concurrent.Future

trait APITokenService {

  def find(id: String): Future[Option[APIToken]]

  def createFor(user: User, label: String): Future[APIToken]

  def allFor(user: User): Future[Seq[APIToken]]

  def use(token: APIToken): Future[APIToken]

  def revoke(token: APIToken): Future[APIToken]

}
