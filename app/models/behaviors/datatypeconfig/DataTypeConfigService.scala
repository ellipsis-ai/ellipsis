package models.behaviors.datatypeconfig

import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.behaviorversion.BehaviorVersion
import slick.dbio.DBIO

import scala.concurrent.Future

trait DataTypeConfigService {

  def allForAction(groupVersion: BehaviorGroupVersion): DBIO[Seq[DataTypeConfig]]

  def allFor(groupVersion: BehaviorGroupVersion): Future[Seq[DataTypeConfig]]

  def createForAction(behaviorVersion: BehaviorVersion): DBIO[DataTypeConfig]

}
