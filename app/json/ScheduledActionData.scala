package json

import models.behaviors.scheduling.scheduledbehavior.ScheduledBehavior
import models.behaviors.scheduling.scheduledmessage.ScheduledMessage
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

case class ScheduledActionArgumentData(name: String, value: String)

case class ScheduledActionData(
  actionName: Option[String],
  trigger: Option[String],
  arguments: Seq[ScheduledActionArgumentData],
  recurrenceString: String,
  useDM: Boolean,
  channel: Option[String]
)

case class ScheduledActionsData(teamId: String, scheduledActions: Seq[ScheduledActionData])

object ScheduledActionsData {
  def fromScheduleData(teamId: String, dataService: DataService, scheduledMessages: Seq[ScheduledMessage], scheduledBehaviors: Seq[ScheduledBehavior])(implicit ec: ExecutionContext): Future[ScheduledActionsData] = {

    val fromMessages = Future.sequence(scheduledMessages.map { ea =>
      Future(ScheduledActionData(None, Some(ea.text), Seq(), ea.recurrence.displayString, ea.isForIndividualMembers, ea.maybeChannel))
    })

    val fromBehaviors = Future.sequence(scheduledBehaviors.map { ea =>
      ea.displayText(dataService).map { name =>
        val arguments = ea.arguments.map { case(key, value) => ScheduledActionArgumentData(key, value) }.toSeq
        ScheduledActionData(Some(name), None, arguments, ea.recurrence.displayString, ea.isForIndividualMembers, ea.maybeChannel)
      }
    })

    for {
      messages <- fromMessages
      behaviors <- fromBehaviors
    } yield {
      ScheduledActionsData(teamId, messages ++ behaviors)
    }
  }
}
