package models.behaviors.behaviorgroupversion

import json.BehaviorGroupData
import models.accounts.user.User
import models.behaviors.behaviorgroup.BehaviorGroup

import scala.concurrent.Future

trait BehaviorGroupVersionService {

  def findWithoutAccessCheck(id: String): Future[Option[BehaviorGroupVersion]]

  def allFor(group: BehaviorGroup): Future[Seq[BehaviorGroupVersion]]

  def createFor(group: BehaviorGroup, user: User): Future[BehaviorGroupVersion]

  def createFor(
                 group: BehaviorGroup,
                 user: User,
                 data: BehaviorGroupData
               ): Future[BehaviorGroupVersion]

}
