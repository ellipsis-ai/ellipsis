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
                                behaviorName: Option[String],
                                behaviorGroupName: Option[String],
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
          behaviorName = None,
          behaviorGroupName = None,
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
      for {
        maybeBehaviorName <- ea.maybeBehaviorName(dataService)
        maybeBehaviorGroupName <- ea.maybeBehaviorGroupName(dataService)
      } yield {
        val arguments = ea.arguments.map { case (key, value) => ScheduledActionArgumentData(key, value) }.toSeq
        ScheduledActionData(
          id = ea.id,
          scheduleType = "behavior",
          behaviorName = maybeBehaviorName,
          behaviorGroupName = maybeBehaviorGroupName,
          behaviorId = Some(ea.behavior.id),
          behaviorGroupId = Some(ea.behavior.group.id),
          trigger = None,
          arguments = arguments,
          recurrence = ScheduledActionRecurrenceData.fromRecurrence(ea.recurrence),
          firstRecurrence = ea.nextSentAt,
          secondRecurrence = ea.followingSentAt,
          useDM = ea.isForIndividualMembers,
          channel = ea.maybeChannel.getOrElse("")
        )
      }
    }
    Future.sequence(data)
  }
}
