package models.behaviors.managedbehaviorgroup

import models.accounts.user.User
import models.behaviors.behaviorgroup.BehaviorGroup
import models.team.Team
import slick.dbio.DBIO

import scala.concurrent.Future

trait ManagedBehaviorGroupService {

  def maybeForAction(group: BehaviorGroup): DBIO[Option[ManagedBehaviorGroup]]

  def maybeFor(group: BehaviorGroup): Future[Option[ManagedBehaviorGroup]]

  def allFor(team: Team): Future[Seq[ManagedBehaviorGroup]]

  def ensureFor(group: BehaviorGroup, maybeContact: Option[User]): Future[ManagedBehaviorGroup]

  def deleteFor(group: BehaviorGroup): Future[Unit]

}
