package models.behaviors.behaviortestresult

import models.behaviors.behaviorgroupversion.BehaviorGroupVersion

import scala.concurrent.Future

trait BehaviorTestResultService {

  def allFor(behaviorGroupVersion: BehaviorGroupVersion): Future[Seq[BehaviorTestResult]]

}
