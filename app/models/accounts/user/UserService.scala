package models.accounts.user

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import models.team.Team
import models.bots.Behavior
import models.bots.events.MessageContext

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait UserService extends IdentityService[User] {
  def find(id: String): Future[Option[User]]
  def findFromMessageContext(context: MessageContext, team: Team): Future[Option[User]]
  def createFor(teamId: String): Future[User]
  def save(user: User): Future[User]
  def ensureUserFor(loginInfo: LoginInfo, teamId: String): Future[User]
  def teamAccessFor(user: User, maybeTargetTeamId: Option[String]): Future[UserTeamAccess]
  def canAccess(user: User, behavior: Behavior): Future[Boolean] = canAccess(user, behavior.team)
  def canAccess(user: User, team: Team): Future[Boolean] = {
    teamAccessFor(user, Some(team.id)).map(_.canAccessTargetTeam)
  }
}
