package models.behaviors.behaviortestresult

import models.behaviors.behaviorversion.BehaviorVersion

import scala.concurrent.Future

trait BehaviorTestResultService {

  def ensureFor(behaviorVersion: BehaviorVersion): Future[BehaviorTestResult]

}
