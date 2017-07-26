package models.accounts.user

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import models.behaviors.behavior.Behavior
import models.behaviors.events.{Event, SlackMessageEvent}
import models.team.Team
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait UserService extends IdentityService[User] {
  def find(id: String): Future[Option[User]]
  def findFromEvent(event: Event, team: Team): Future[Option[User]]
  def createFor(teamId: String): Future[User]
  def save(user: User): Future[User]
  def ensureUserForAction(loginInfo: LoginInfo, teamId: String): DBIO[User]
  def ensureUserFor(loginInfo: LoginInfo, teamId: String): Future[User]
  def teamAccessForAction(user: User, maybeTargetTeamId: Option[String]): DBIO[UserTeamAccess]
  def teamAccessFor(user: User, maybeTargetTeamId: Option[String]): Future[UserTeamAccess]
  def canAccessAction(user: User, behavior: Behavior): DBIO[Boolean] = canAccessAction(user, behavior.team)
  def canAccess(user: User, behavior: Behavior): Future[Boolean] = canAccess(user, behavior.team)
  def canAccessAction(user: User, team: Team): DBIO[Boolean] = {
    teamAccessForAction(user, Some(team.id)).map(_.canAccessTargetTeam)
  }
  def canAccess(user: User, team: Team): Future[Boolean] = {
    teamAccessFor(user, Some(team.id)).map(_.canAccessTargetTeam)
  }
  def isAdmin(user: User): Future[Boolean]

  def maybeNameFor(user: User, event: SlackMessageEvent): Future[Option[String]]

  def findForInvocationToken(tokenId: String): Future[Option[User]]
}
