package models.accounts.user

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import json.{SlackUserData, UserData}
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.events.Event
import models.team.Team
import slick.dbio.DBIO

import scala.concurrent.{ExecutionContext, Future}

trait UserService extends IdentityService[User] {
  def find(id: String): Future[Option[User]]
  def findFromEvent(event: Event, team: Team): Future[Option[User]]
  def createFor(teamId: String): Future[User]
  def save(user: User): Future[User]
  def ensureUserForAction(loginInfo: LoginInfo, teamId: String): DBIO[User]
  def ensureUserFor(loginInfo: LoginInfo, teamId: String): Future[User]
  def teamAccessForAction(user: User, maybeTargetTeamId: Option[String]): DBIO[UserTeamAccess]
  def teamAccessFor(user: User, maybeTargetTeamId: Option[String]): Future[UserTeamAccess]
  def canAccess(user: User, group: BehaviorGroup)(implicit ec: ExecutionContext): Future[Boolean] = canAccess(user, group.team)
  def canAccessAction(user: User, group: BehaviorGroup)(implicit ec: ExecutionContext): DBIO[Boolean] = canAccessAction(user, group.team)
  def canAccessAction(user: User, behavior: Behavior)(implicit ec: ExecutionContext): DBIO[Boolean] = canAccessAction(user, behavior.team)
  def canAccess(user: User, behavior: Behavior)(implicit ec: ExecutionContext): Future[Boolean] = canAccess(user, behavior.team)
  def canAccessAction(user: User, team: Team)(implicit ec: ExecutionContext): DBIO[Boolean] = {
    teamAccessForAction(user, Some(team.id)).map(_.canAccessTargetTeam)
  }
  def canAccess(user: User, team: Team)(implicit ec: ExecutionContext): Future[Boolean] = {
    teamAccessFor(user, Some(team.id)).map(_.canAccessTargetTeam)
  }
  def isAdmin(user: User): Future[Boolean]

  def userDataFor(user: User, team: Team): Future[UserData]

  def findForInvocationToken(tokenId: String): Future[Option[User]]
}
