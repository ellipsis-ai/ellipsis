package models.behaviors.invocationtoken

import models.accounts.user.User
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.scheduling.Scheduled
import slick.dbio.DBIO

import scala.concurrent.Future

trait InvocationTokenService {

  def findNotExpired(id: String): Future[Option[InvocationToken]]

  def createForAction(
                       user: User,
                       behaviorVersion: BehaviorVersion,
                       maybeScheduled: Option[Scheduled],
                       maybeTeamIdForContext: Option[String]
                     ): DBIO[InvocationToken]

}
