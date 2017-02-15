package json

import models.behaviors.scheduledmessage.ScheduledMessage
import models.behaviors.behavior.Behavior
import models.environmentvariable.EnvironmentVariable

case class ScheduledActionData(text: String)

object ScheduledActionData {
}

case class ScheduledActionsData(teamId: String, scheduledActions: Seq[ScheduledActionData])
