package models.behaviors.invocationtoken

import models.accounts.user.User

import scala.concurrent.Future

trait InvocationTokenService {

  def find(id: String): Future[Option[InvocationToken]]

  def createFor(user: User): Future[InvocationToken]

}
