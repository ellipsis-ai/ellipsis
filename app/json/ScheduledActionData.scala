package json

case class ScheduledActionData(text: String)

object ScheduledActionData {
}

case class ScheduledActionsData(teamId: String, scheduledActions: Seq[ScheduledActionData])
