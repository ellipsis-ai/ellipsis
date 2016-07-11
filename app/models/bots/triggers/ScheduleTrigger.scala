package models.bots.triggers

import models.bots.{TimeEvent, Event, BehaviorVersion}
import org.joda.time.DateTime

case class ScheduleTrigger(
                            id: String,
                            behaviorVersion: BehaviorVersion,
                            recurrence: Recurrence,
                            maybeLastTriggered: Option[DateTime],
                            createdAt: DateTime
                            ) extends Trigger {



  def isActivatedBy(event: Event): Boolean = {
    event match {
      case e: TimeEvent => recurrence.nextAfter(maybeLastTriggered.getOrElse(createdAt)).isBefore(DateTime.now)
      case _ => false
    }
  }
}
