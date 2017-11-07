package models.behaviors.events

import utils.Enum
import drivers.SlickPostgresDriver.api._

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

/* N.B. Don't forget to add new EventTypes to the list of EventType values */

object EventType extends Enum[EventType] {
  val values = List(ScheduledEventType, ApiEventType, TestEventType, ChatEventType, WebEventType)
  def maybeFrom(maybeString: Option[String]): Option[EventType] = maybeString.flatMap(find)
}
