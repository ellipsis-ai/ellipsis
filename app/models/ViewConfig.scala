package models

import models.accounts.user.UserTeamAccess
import play.api.Configuration

case class ViewConfig(
                          configuration: Configuration,
                          maybeTeamAccess: Option[UserTeamAccess]
                         ) {

  val isDevelopment = configuration.getString("application.version").contains("Development")

  val isProduction = !isDevelopment

  val maybeTargetTeamId = maybeTeamAccess.flatMap(_.maybeTargetTeam.map(_.id))
}
