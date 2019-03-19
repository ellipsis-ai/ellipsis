package models.behaviors.ellipsisobject

import models.behaviors.events.Event

case class EventInfo(
                      user: EventUser,
                      originalEventType: String,
                      platformName: String,
                      platformDescription: String,
                      message: Option[MessageObject]
                    )

object EventInfo {

  def buildFor(
                event: Event,
                user: EventUser,
                maybeMessage: Option[MessageObject]
              ): EventInfo = {
    EventInfo(
      user,
      event.originalEventType.toString,
      event.eventContext.name,
      event.eventContext.description,
      maybeMessage
    )
  }
}
