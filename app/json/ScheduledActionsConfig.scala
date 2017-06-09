package json

case class ScheduledActionsConfig(
                                   containerId: String,
                                   teamId: String,
                                   scheduledActions: Seq[ScheduledActionData],
                                   channelList: Seq[ScheduleChannelData],
                                   teamTimeZone: Option[String]
                                 )
