package models.bots.behaviorparameter

import json.BehaviorParameterData
import models.bots.behaviorversion.BehaviorVersion

import scala.concurrent.Future

trait BehaviorParameterService {

  def allFor(behaviorVersion: BehaviorVersion): Future[Seq[BehaviorParameter]]

  def ensureFor(behaviorVersion: BehaviorVersion, params: Seq[BehaviorParameterData]): Future[Seq[BehaviorParameter]]

}
