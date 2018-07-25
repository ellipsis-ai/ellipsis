package models.behaviors.behaviortestresult

import akka.actor.ActorSystem
import models.behaviors.behaviorversion.BehaviorVersion

import scala.concurrent.{ExecutionContext, Future}

trait BehaviorTestResultService {

  def ensureFor(behaviorVersion: BehaviorVersion)
               (implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[BehaviorTestResult]

}
