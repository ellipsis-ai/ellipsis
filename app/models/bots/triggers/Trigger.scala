package models.bots.triggers

import models.bots.{Behavior, Event}

trait Trigger {
  val id: String
  val behavior: Behavior

  def isActivatedBy(event: Event): Boolean
}
