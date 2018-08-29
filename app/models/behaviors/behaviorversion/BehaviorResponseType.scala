package models.behaviors.behaviorversion

import utils.Enum

object BehaviorResponseType extends Enum[BehaviorResponseType] {
  val values = List(Normal, Private, Threaded)
  def definitelyFind(name: String): BehaviorResponseType = find(name).getOrElse(Normal)
  def definitelyFind(maybeName: Option[String]): BehaviorResponseType = maybeName.map(definitelyFind).getOrElse(Normal)
}

sealed trait BehaviorResponseType extends BehaviorResponseType.Value {
  val displayName: String
}

case object Normal extends BehaviorResponseType {
  val displayName = "Respond normally"
}

case object Private extends BehaviorResponseType {
  val displayName = "Respond privately"
}

case object Threaded extends BehaviorResponseType {
  val displayName = "Respond in a new thread"
}
