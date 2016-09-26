package models.behaviors.behaviorparameter

import models.team.Team

import scala.concurrent.Future

trait BehaviorParameterTypeService {

  def allFor(team: Team): Future[Seq[BehaviorParameterType]]

  def isValid(text: String, parameterType: BehaviorParameterType): Future[Boolean]

}
