package models.bots.triggers

import models.bots.{BehaviorVersion, Event}

trait Trigger {
  val id: String
  val behaviorVersion: BehaviorVersion

  def isActivatedBy(event: Event): Boolean
}
