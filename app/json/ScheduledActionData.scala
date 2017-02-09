package json

import models.behaviors.scheduledmessage.ScheduledMessage
import models.behaviors.behavior.Behavior

case class ScheduledActionData(name: String)

object ScheduledActionData {}

case class ScheduledActionsData(teamId: String, scheduledActions: Seq[ScheduledActionData])
