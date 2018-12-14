package models.behaviors.ellipsisobject

import json.Formatting._
import models.behaviors.events.Event
import play.api.libs.json._

case class EventInfo(event: Event, user: EventUser, maybeMessage: Option[Message]) {
  def toJson: JsObject = {
    val messagePart = maybeMessage.map { m =>
      Json.obj("message" -> Json.toJsObject(m))
    }.getOrElse(Json.obj())
    Json.obj(
      "user" -> user.toJson,
      "originalEventType" -> event.originalEventType.toString,
      "platformName" -> event.eventContext.name,
      "platformDescription" -> event.eventContext.description
    ) ++ messagePart
  }
}
