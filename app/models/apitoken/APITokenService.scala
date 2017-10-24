package models.apitoken

import models.accounts.user.User
import models.behaviors.invocationtoken.InvocationToken

import scala.concurrent.Future

trait APITokenService {

  def find(id: String): Future[Option[APIToken]]

  def createFor(user: User, label: String): Future[APIToken]

  def createFor(invocationToken: InvocationToken, maybeExpirySeconds: Option[Int], isOneTime: Boolean): Future[APIToken]

  def allDisplayableFor(user: User): Future[Seq[APIToken]]

  def use(token: APIToken): Future[APIToken]

  def revoke(token: APIToken): Future[APIToken]

}
