package models.behaviors.events

import utils.Enum

sealed trait EventType extends EventType.Value

object EventType extends Enum[EventType] {
  case object scheduled extends EventType
  case object api extends EventType
  case object externalApi extends EventType
  case object actionChoice extends EventType
  case object nextAction extends EventType
  case object test extends EventType
  case object chat extends EventType
  case object web extends EventType
  case object dialog extends EventType

  val values = List(scheduled, api, externalApi, actionChoice, nextAction, test, chat, web, dialog)
  def maybeFrom(maybeString: Option[String]): Option[EventType] = maybeString.flatMap(find)
}
