package models.behaviors.triggers

import models.behaviors.behaviorversion.BehaviorVersion

trait Trigger {
  val id: String
  val behaviorVersion: BehaviorVersion
}
