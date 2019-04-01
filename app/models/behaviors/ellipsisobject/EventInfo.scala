package models.behaviors.ellipsisobject

import models.behaviors.events.Event
import play.api.Configuration

case class EventInfo(
                      user: EventUser,
                      originalEventType: String,
                      platformName: String,
                      platformDescription: String,
                      message: Option[MessageObject],
                      schedule: Option[ScheduleInfo]
                    )

object EventInfo {

  def buildFor(
                event: Event,
                user: EventUser,
                maybeMessage: Option[MessageObject],
                configuration: Configuration
              ): EventInfo = {
    EventInfo(
      user,
      event.originalEventType.toString,
      event.eventContext.name,
      event.eventContext.description,
      maybeMessage,
      ScheduleInfo.maybeBuildFor(event, configuration)
    )
  }
}
