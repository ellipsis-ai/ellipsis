package services.ms_teams.apiModels

case class Team(
                  id: String,
                  displayName: Option[String],
                  description: Option[String]
                )
