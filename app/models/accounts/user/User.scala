package models.accounts.user

import com.mohiva.play.silhouette.api.{Identity, LoginInfo}
import models.Team
import models.accounts.LinkedAccount
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global

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

  def canAccess(team: Team): DBIO[Boolean] = teamAccessFor(Some(team.id)).map { teamAccess =>
    teamAccess.canAccessTargetTeam
  }

  def teamAccessFor(maybeTargetTeamId: Option[String]): DBIO[UserTeamAccess] = {
    for {
      loggedInTeam <- Team.find(teamId).map(_.get)
      maybeSlackLinkedAccount <- LinkedAccount.maybeForSlackFor(this)
      isAdmin <- maybeSlackLinkedAccount.map(_.isAdmin).getOrElse(DBIO.successful(false))
      maybeTeam <- maybeTargetTeamId.map { targetTeamId =>
        if (targetTeamId != teamId && !isAdmin) {
          DBIO.successful(None)
        } else {
          Team.find(targetTeamId)
        }
      }.getOrElse {
        Team.find(teamId)
      }
    } yield UserTeamAccess(this, loggedInTeam, maybeTeam, maybeTeam.exists(t => t.id != this.teamId))
  }

  def loginInfo: LoginInfo = LoginInfo(User.EPHEMERAL_USER_ID, id)

}

object User {
  val EPHEMERAL_USER_ID = "EPHEMERAL"
}
