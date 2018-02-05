package models.behaviors.behaviorgroupversionsha

import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import slick.dbio.DBIO

import scala.concurrent.Future

trait BehaviorGroupVersionSHAService {

  def findForId(groupVersionId: String): Future[Option[BehaviorGroupVersionSHA]]

  def createForAction(groupVersion: BehaviorGroupVersion, gitSHA: String): DBIO[BehaviorGroupVersionSHA]

  def maybeCreateFor(group: BehaviorGroup, gitSHA: String): Future[Option[BehaviorGroupVersionSHA]]

}
