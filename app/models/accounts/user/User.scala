package models.accounts.user

import com.mohiva.play.silhouette.api.{Identity, LoginInfo}
import models.team.Team

case class UserTeamAccess(user: User, loggedInTeam: Team, maybeTargetTeam: Option[Team], isAdminAccess: Boolean) {

  val maybeAdminAccessToTeam: Option[Team] = {
    if (isAdminAccess) {
      maybeTargetTeam
    } else {
      None
    }
  }

  val maybeAdminAccessToTeamId = maybeAdminAccessToTeam.map(_.id)

  val canAccessTargetTeam: Boolean = maybeTargetTeam.isDefined

}

case class User(
                 id: String,
                 teamId: String,
                 maybeEmail: Option[String]
                 ) extends Identity {

  def loginInfo: LoginInfo = LoginInfo(User.EPHEMERAL_USER_ID, id)

}

object User {
  val EPHEMERAL_USER_ID = "EPHEMERAL"
}
