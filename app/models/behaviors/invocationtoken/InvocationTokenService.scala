package models.behaviors.invocationtoken

import models.accounts.user.User
import models.behaviors.behavior.Behavior
import models.behaviors.scheduledmessage.ScheduledMessage

import scala.concurrent.Future

trait InvocationTokenService {

  def findNotExpired(id: String): Future[Option[InvocationToken]]

  def createFor(user: User, behavior: Behavior, maybeScheduledMessage: Option[ScheduledMessage]): Future[InvocationToken]

}
