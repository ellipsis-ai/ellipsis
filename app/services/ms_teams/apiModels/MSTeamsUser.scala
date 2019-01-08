package services.ms_teams.apiModels

case class MSTeamsUser(
                         id: String,
                         displayName: Option[String],
                         givenName: Option[String],
                         surname: Option[String],
                         mail: Option[String],
                         mailBoxSettings: Option[MailBoxSettings]
                      ) {
  val formattedLink: Option[String] = displayName.map(d => s"<at>$d</at>")
}
