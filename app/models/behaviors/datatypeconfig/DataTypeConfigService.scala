package models.behaviors.datatypeconfig

import json.DataTypeConfigData
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.behaviorversion.BehaviorVersion
import slick.dbio.DBIO

import scala.concurrent.Future

trait DataTypeConfigService {

  def allForAction(groupVersion: BehaviorGroupVersion): DBIO[Seq[DataTypeConfig]]

  def allFor(groupVersion: BehaviorGroupVersion): Future[Seq[DataTypeConfig]]

  def maybeFor(behaviorVersion: BehaviorVersion): Future[Option[DataTypeConfig]]

  def createForAction(behaviorVersion: BehaviorVersion, data: DataTypeConfigData): DBIO[DataTypeConfig]

}
