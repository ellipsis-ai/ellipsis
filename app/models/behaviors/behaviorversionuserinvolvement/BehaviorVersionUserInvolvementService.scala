package models.behaviors.behaviorversionuserinvolvement

import java.time.OffsetDateTime

import models.accounts.user.User
import models.behaviors.behaviorversion.BehaviorVersion
import models.team.Team

import scala.concurrent.Future

trait BehaviorVersionUserInvolvementService {

  def createAllFor(
                     behaviorVersion: BehaviorVersion,
                     users: Seq[User],
                     when: OffsetDateTime
                 ): Future[Seq[BehaviorVersionUserInvolvement]]

  def findAllForTeamBetween(team: Team, start: OffsetDateTime, end: OffsetDateTime): Future[Seq[BehaviorVersionUserInvolvement]]

}
