package models.behaviors.behaviorparameter

import json.BehaviorParameterData
import models.behaviors.behaviorversion.BehaviorVersion
import slick.dbio.DBIO

import scala.concurrent.Future

trait BehaviorParameterService {

  def allFor(behaviorVersion: BehaviorVersion): Future[Seq[BehaviorParameter]]

  def ensureForAction(behaviorVersion: BehaviorVersion, params: Seq[BehaviorParameterData]): DBIO[Seq[BehaviorParameter]]

  def isFirstForBehaviorVersion(parameter: BehaviorParameter): Future[Boolean]

}
