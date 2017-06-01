package json

case class ScheduledActionsConfig(teamId: String, scheduledActions: Seq[ScheduledActionData], teamTimeZone: Option[String])
