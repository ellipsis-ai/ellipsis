package models.accounts.user

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import json.{SlackUserData, UserData}
import models.accounts.ms_teams.profile.MSTeamsProfile
import models.accounts.slack.SlackUserTeamIds
import models.accounts.slack.profile.SlackProfile
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.events.Event
import models.team.Team
import slick.dbio.DBIO

import scala.concurrent.{ExecutionContext, Future}

trait UserService extends IdentityService[User] {
  def findAction(id: String): DBIO[Option[User]]
  def find(id: String): Future[Option[User]]
  def findFromEvent(event: Event, team: Team): Future[Option[User]]
  def createFor(teamId: String): Future[User]
  def save(user: User): Future[User]
  def ensureUserForAction(loginInfo: LoginInfo, otherLoginInfos: Seq[LoginInfo], teamId: String): DBIO[User]
  def ensureUserFor(loginInfo: LoginInfo, otherLoginInfos: Seq[LoginInfo], teamId: String): Future[User]
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

  def userDataForAction(user: User, team: Team): DBIO[UserData]
  def userDataFor(user: User, team: Team): Future[UserData]

  def maybeUserDataForEmail(email: String, team: Team): Future[Option[UserData]]

  def maybeSlackTeamIdsFor(user: User): Future[Option[SlackUserTeamIds]]

  def maybeSlackUserDataFor(user: User): Future[Option[SlackUserData]]

  def maybeSlackProfileFor(user: User): Future[Option[SlackProfile]]

  def maybeMSTeamsProfileFor(user: User): Future[Option[MSTeamsProfile]]

  def findForInvocationToken(tokenId: String): Future[Option[User]]

  def allFor(team: Team): Future[Seq[User]]
}
