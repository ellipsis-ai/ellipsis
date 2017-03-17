package models.behaviors.behaviorparameter

import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.input.Input
import slick.dbio.DBIO

import scala.concurrent.Future

trait BehaviorParameterService {

  def allFor(behaviorVersion: BehaviorVersion): Future[Seq[BehaviorParameter]]

  def ensureForAction(behaviorVersion: BehaviorVersion, inputs: Seq[Input]): DBIO[Seq[BehaviorParameter]]

  def isFirstForBehaviorVersion(parameter: BehaviorParameter): Future[Boolean]

}
