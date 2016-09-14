package models.bots.behaviorparameter

import models.bots.behaviorversion.BehaviorVersion

import scala.concurrent.Future

trait BehaviorParameterService {

  def allFor(behaviorVersion: BehaviorVersion): Future[Seq[BehaviorParameter]]

  def ensureFor(behaviorVersion: BehaviorVersion, params: Seq[(String, Option[String])]): Future[Seq[BehaviorParameter]]

}
