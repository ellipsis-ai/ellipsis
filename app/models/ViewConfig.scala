package models

import controllers.RemoteAssets
import models.accounts.user.UserTeamAccess
import models.team.Team
import play.api.Configuration

case class ViewConfig(
                       assets: RemoteAssets,
                       maybeTeamAccess: Option[UserTeamAccess]
                     ) {

  val configuration: Configuration = assets.configuration

  val isDevelopment: Boolean = configuration.get[String]("application.version").contains("Development")

  val isProduction: Boolean = !isDevelopment

  val maybeTargetTeamId: Option[String] = maybeTeamAccess.flatMap(_.maybeTargetTeam.map(_.id))

  val maybeAdminAccessTeamId: Option[String] = maybeTeamAccess.flatMap(_.maybeAdminAccessToTeamId)

  val isAdmin: Boolean = maybeTeamAccess.exists(_.isAdminAccess)

  val botName: String = maybeTeamAccess.flatMap(_.maybeBotName).getOrElse(Team.defaultBotName)

}
