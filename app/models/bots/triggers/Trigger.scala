package models.bots.triggers

import models.bots.BehaviorVersion

trait Trigger {
  val id: String
  val behaviorVersion: BehaviorVersion
}
