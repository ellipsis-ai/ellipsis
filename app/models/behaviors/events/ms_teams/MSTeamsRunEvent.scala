package models.behaviors.events.ms_teams

import models.behaviors.ActionArg
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.events.{Event, EventType, MSTeamsEventContext, RunEvent}
import models.behaviors.scheduling.Scheduled

case class MSTeamsRunEvent(
                          eventContext: MSTeamsEventContext,
                          behaviorVersion: BehaviorVersion,
                          arguments: Seq[ActionArg],
                          eventType: EventType,
                          maybeOriginalEventType: Option[EventType],
                          maybeScheduled: Option[Scheduled],
                          override val isEphemeral: Boolean,
                          override val maybeResponseUrl: Option[String],
                          maybeTriggeringMessageTs: Option[String]
                        ) extends RunEvent {

  override type EC = MSTeamsEventContext

  val maybeMessageId: Option[String] = maybeTriggeringMessageTs

  def withOriginalEventType(originalEventType: EventType, isUninterrupted: Boolean): Event = {
    this.copy(maybeOriginalEventType = Some(originalEventType))
  }

}
