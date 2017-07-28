package models.accounts.linkedsimpletoken

import models.accounts.user.User
import slick.dbio.DBIO

import scala.concurrent.Future

trait LinkedSimpleTokenService {

  def allForUserAction(user: User): DBIO[Seq[LinkedSimpleToken]]

  def allForUser(user: User): Future[Seq[LinkedSimpleToken]]

  def save(token: LinkedSimpleToken): Future[LinkedSimpleToken]

}
