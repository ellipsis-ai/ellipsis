package models.behaviors.datatypefield

import models.behaviors.behaviorparameter.BehaviorParameterType
import models.behaviors.datatypeconfig.DataTypeConfig

import scala.concurrent.Future

trait DataTypeFieldService {

  def allFor(config: DataTypeConfig): Future[Seq[DataTypeField]]

  def createFor(name: String, fieldType: BehaviorParameterType, config: DataTypeConfig): Future[DataTypeField]

}
