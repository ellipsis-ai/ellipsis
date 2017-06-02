package json

case class ScheduledActionsConfig(
                                   containerId: String,
                                   teamId: String,
                                   scheduledActions: Seq[ScheduledActionData],
                                   teamTimeZone: Option[String]
                                 )
