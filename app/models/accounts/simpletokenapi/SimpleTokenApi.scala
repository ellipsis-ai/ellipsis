package models.accounts.simpletokenapi

case class SimpleTokenApi(
                          id: String,
                          name: String,
                          maybeTokenUrl: Option[String],
                          maybeTeamId: Option[String]
                        )
