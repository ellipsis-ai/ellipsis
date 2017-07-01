package models.behaviors.datatypefield

import json.DataTypeFieldData
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.datatypeconfig.DataTypeConfig
import slick.dbio.DBIO

import scala.concurrent.Future

trait DataTypeFieldService {

  def allFor(config: DataTypeConfig): Future[Seq[DataTypeField]]

  def allForAction(config: DataTypeConfig): DBIO[Seq[DataTypeField]]

  def createForAction(data: DataTypeFieldData, rank: Int, config: DataTypeConfig, behaviorGroupVersion: BehaviorGroupVersion): DBIO[DataTypeField]

}
