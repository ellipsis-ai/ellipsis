package models.behaviors.invocationtoken

import models.accounts.user.User
import models.behaviors.behavior.Behavior
import models.behaviors.scheduling.Scheduled
import slick.dbio.DBIO

import scala.concurrent.Future

trait InvocationTokenService {

  def findNotExpired(id: String): Future[Option[InvocationToken]]

  def createForAction(user: User, behavior: Behavior, maybeScheduled: Option[Scheduled]): DBIO[InvocationToken]

}
