package json

import java.time.OffsetDateTime

import models.behaviors.scheduling.scheduledbehavior.ScheduledBehavior
import models.behaviors.scheduling.scheduledmessage.ScheduledMessage
import services.DataService
import utils._

import scala.concurrent.{ExecutionContext, Future}

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
  def fromScheduledMessages(
                             scheduledMessages: Seq[ScheduledMessage],
                             channelList: Seq[ChannelLike]
                           )(implicit ec: ExecutionContext): Future[Seq[ScheduledActionData]] = {
    val data = scheduledMessages.map { ea =>
      Future.successful(
        ScheduledActionData(
          id = ea.id,
          scheduleType = "message",
          behaviorId = None,
          behaviorGroupId = None,
          trigger = Some(ea.text),
          arguments = Seq(),
          recurrence = ScheduledActionRecurrenceData.fromRecurrence(ea.recurrence),
          firstRecurrence = ea.nextSentAt,
          secondRecurrence = ea.followingSentAt,
          useDM = ea.isForIndividualMembers,
          channel = ea.maybeChannel.getOrElse("")
        )
      )
    }
    Future.sequence(data)
  }

  def fromScheduledBehaviors(
                              scheduledBehaviors: Seq[ScheduledBehavior],
                              dataService: DataService,
                              channelList: Seq[ChannelLike]
                            )(implicit ec: ExecutionContext): Future[Seq[ScheduledActionData]] = {
    val data = scheduledBehaviors.map { ea =>
      val arguments = ea.arguments.map { case (key, value) => ScheduledActionArgumentData(key, value) }.toSeq
      Future.successful(ScheduledActionData(
        id = ea.id,
        scheduleType = "behavior",
        behaviorId = Some(ea.behavior.id),
        behaviorGroupId = Some(ea.behavior.group.id),
        trigger = None,
        arguments = arguments,
        recurrence = ScheduledActionRecurrenceData.fromRecurrence(ea.recurrence),
        firstRecurrence = ea.nextSentAt,
        secondRecurrence = ea.followingSentAt,
        useDM = ea.isForIndividualMembers,
        channel = ea.maybeChannel.getOrElse("")
      ))
    }
    Future.sequence(data)
  }
}
