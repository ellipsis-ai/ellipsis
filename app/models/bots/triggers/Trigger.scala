package models.bots.triggers

import models.bots.behaviorversion.BehaviorVersion

trait Trigger {
  val id: String
  val behaviorVersion: BehaviorVersion
}
