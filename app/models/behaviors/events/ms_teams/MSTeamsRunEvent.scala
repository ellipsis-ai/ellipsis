package models.behaviors.events.ms_teams

import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.events.{Event, EventType, MSTeamsEventContext, RunEvent}

case class MSTeamsRunEvent(
                          eventContext: MSTeamsEventContext,
                          behaviorVersion: BehaviorVersion,
                          arguments: Map[String, String],
                          maybeOriginalEventType: Option[EventType],
                          override val isEphemeral: Boolean,
                          override val maybeResponseUrl: Option[String],
                          maybeTriggeringMessageTs: Option[String]
                        ) extends RunEvent {

  override type EC = MSTeamsEventContext

  val eventType: EventType = EventType.api
  val maybeMessageIdForReaction: Option[String] = maybeTriggeringMessageTs

  def withOriginalEventType(originalEventType: EventType, isUninterrupted: Boolean): Event = {
    this.copy(maybeOriginalEventType = Some(originalEventType))
  }

}
