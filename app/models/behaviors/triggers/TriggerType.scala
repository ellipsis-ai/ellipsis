package models.behaviors.triggers

import utils.Enum

object TriggerType extends Enum[TriggerType] {
  val values = List(MessageSent, ReactionAdded)
  def definitelyFind(name: String): TriggerType = find(name).getOrElse(MessageSent)
}

sealed trait TriggerType extends TriggerType.Value {
  val requiresAuth: Boolean
}

case object MessageSent extends TriggerType {
  val requiresAuth = true
}

case object ReactionAdded extends TriggerType {
  val requiresAuth = false

}
