package json

case class ApplicationIndexConfig(
  containerId: String,
  behaviorGroups: Seq[BehaviorGroupData],
  csrfToken: Option[String],
  teamId: String,
  slackTeamId: Option[String],
  teamTimeZone: Option[String],
  branchName: Option[String]
)
