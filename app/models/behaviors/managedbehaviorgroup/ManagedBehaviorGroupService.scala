package models.behaviors.managedbehaviorgroup

import models.behaviors.behaviorgroup.BehaviorGroup
import slick.dbio.DBIO

import scala.concurrent.Future

trait ManagedBehaviorGroupService {

  def maybeForAction(group: BehaviorGroup): DBIO[Option[ManagedBehaviorGroup]]

  def maybeFor(group: BehaviorGroup): Future[Option[ManagedBehaviorGroup]]

  def ensureForAction(group: BehaviorGroup): DBIO[ManagedBehaviorGroup]

}
