package models.behaviors.events

import utils.Enum

object EventType extends Enum[EventType] {
  val values = List(ScheduledEventType, ApiEventType, TestEventType, ChatEventType)
  def maybeFrom(maybeString: Option[String]): Option[EventType] = maybeString.flatMap(find)
}

sealed trait EventType extends EventType.Value {
  val value: String
}

case object ScheduledEventType extends EventType {
  val value: String = "scheduled"
}

case object ApiEventType extends EventType {
  val value: String = "api"
}

case object TestEventType extends EventType {
  val value: String = "test"
}

case object ChatEventType extends EventType {
  val value: String = "chat"
}

case object WebEventType extends EventType {
  val value: String = "web"
}
