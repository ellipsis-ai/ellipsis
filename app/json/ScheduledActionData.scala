package json

import java.time.OffsetDateTime

import models.behaviors.scheduling.scheduledbehavior.ScheduledBehavior
import models.behaviors.scheduling.scheduledmessage.ScheduledMessage

case class ScheduledActionArgumentData(name: String, value: String)

case class ScheduledActionData(
                                id: String,
                                scheduleType: String,
                                behaviorId: Option[String],
                                behaviorGroupId: Option[String],
                                trigger: Option[String],
                                arguments: Seq[ScheduledActionArgumentData],
                                recurrence: ScheduledActionRecurrenceData,
                                firstRecurrence: OffsetDateTime,
                                secondRecurrence: OffsetDateTime,
                                useDM: Boolean,
                                channel: String
                              )

object ScheduledActionData {
  def fromScheduledMessage(scheduledMessage: ScheduledMessage): ScheduledActionData = {
    ScheduledActionData(
      id = scheduledMessage.id,
      scheduleType = "message",
      behaviorId = None,
      behaviorGroupId = None,
      trigger = Some(scheduledMessage.text),
      arguments = Seq(),
      recurrence = ScheduledActionRecurrenceData.fromRecurrence(scheduledMessage.recurrence),
      firstRecurrence = scheduledMessage.nextSentAt,
      secondRecurrence = scheduledMessage.followingSentAt,
      useDM = scheduledMessage.isForIndividualMembers,
      channel = scheduledMessage.maybeChannel.getOrElse("")
    )
  }

  def fromScheduledBehavior(scheduledBehavior: ScheduledBehavior): ScheduledActionData = {
    val arguments = scheduledBehavior.arguments.map { case (key, value) => ScheduledActionArgumentData(key, value) }.toSeq
    ScheduledActionData(
      id = scheduledBehavior.id,
      scheduleType = "behavior",
      behaviorId = Some(scheduledBehavior.behavior.id),
      behaviorGroupId = Some(scheduledBehavior.behavior.group.id),
      trigger = None,
      arguments = arguments,
      recurrence = ScheduledActionRecurrenceData.fromRecurrence(scheduledBehavior.recurrence),
      firstRecurrence = scheduledBehavior.nextSentAt,
      secondRecurrence = scheduledBehavior.followingSentAt,
      useDM = scheduledBehavior.isForIndividualMembers,
      channel = scheduledBehavior.maybeChannel.getOrElse("")
    )
  }
}
