package models

import controllers.RemoteAssets
import models.accounts.user.UserTeamAccess
import play.api.Configuration

case class ViewConfig(
                          assets: RemoteAssets,
                          maybeTeamAccess: Option[UserTeamAccess]
                         ) {

  val configuration: Configuration = assets.configuration

  val isDevelopment = configuration.get[String]("application.version").contains("Development")

  val isProduction = !isDevelopment

  val maybeTargetTeamId = maybeTeamAccess.flatMap(_.maybeTargetTeam.map(_.id))
}
