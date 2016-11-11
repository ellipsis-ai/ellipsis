package models.accounts.linkedsimpletoken

import models.accounts.user.User

import scala.concurrent.Future

trait LinkedSimpleTokenService {

  def allForUser(user: User): Future[Seq[LinkedSimpleToken]]

  def save(token: LinkedSimpleToken): Future[LinkedSimpleToken]

}
