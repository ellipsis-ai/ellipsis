package services.ms_teams.apiModels

case class MSTeamsUser(
                         id: String,
                         displayName: Option[String],
                         mail: Option[String],
                         mailBoxSettings: Option[MailBoxSettings]
                      )
