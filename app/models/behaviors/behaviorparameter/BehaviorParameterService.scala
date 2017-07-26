package models.behaviors.behaviorparameter

import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.input.Input
import slick.dbio.DBIO

import scala.concurrent.Future

trait BehaviorParameterService {

  def allForAction(behaviorVersion: BehaviorVersion): DBIO[Seq[BehaviorParameter]]

  def allFor(behaviorVersion: BehaviorVersion): Future[Seq[BehaviorParameter]]

  def ensureForAction(behaviorVersion: BehaviorVersion, inputs: Seq[Input]): DBIO[Seq[BehaviorParameter]]

  def isFirstForBehaviorVersionAction(parameter: BehaviorParameter): DBIO[Boolean]

}
