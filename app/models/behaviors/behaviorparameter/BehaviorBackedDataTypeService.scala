package models.behaviors.behaviorparameter

import models.behaviors.behavior.Behavior
import models.team.Team

import scala.concurrent.Future

trait BehaviorBackedDataTypeService {

  def createFor(name: String, behavior: Behavior): Future[BehaviorBackedDataType]

  def allFor(team: Team): Future[Seq[BehaviorBackedDataType]]

}
