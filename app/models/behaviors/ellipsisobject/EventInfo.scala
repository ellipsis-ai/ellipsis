package models.behaviors.ellipsisobject

import models.behaviors.events.Event
import play.api.libs.json._

case class EventInfo(event: Event) {
  def toJson: JsObject = Json.obj("originalEventType" -> event.originalEventType.toString)
}
