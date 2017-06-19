package json

case class ScheduledActionsConfig(
                                   containerId: String,
                                   csrfToken: Option[String],
                                   teamId: String,
                                   scheduledActions: Seq[ScheduledActionData],
                                   channelList: Seq[ScheduleChannelData],
                                   behaviorGroups: Seq[BehaviorGroupData],
                                   teamTimeZone: Option[String],
                                   teamTimeZoneName: Option[String],
                                   slackUserId: Option[String]
                                 )
