package models.accounts.user

import java.time.OffsetDateTime
import javax.inject.Inject

import akka.actor.ActorSystem
import com.google.inject.Provider
import com.mohiva.play.silhouette.api.LoginInfo
import drivers.SlickPostgresDriver.api._
import models.IDs
import models.accounts.linkedaccount.LinkedAccount
import models.behaviors.events.{Event, SlackMessageEvent}
import models.team.Team
import play.api.Configuration
import services.DataService
import slack.api.ApiError

import scala.concurrent.{ExecutionContext, Future}

class UserServiceImpl @Inject() (
                                  dataServiceProvider: Provider[DataService],
                                  configuration: Configuration,
                                  implicit val actorSystem: ActorSystem,
                                  implicit val ec: ExecutionContext
                                ) extends UserService {

  def dataService = dataServiceProvider.get

  import UserQueries._

  def retrieve(loginInfo: LoginInfo): Future[Option[User]] = {
    if (loginInfo.providerID == User.EPHEMERAL_USER_ID) {
      val userId = loginInfo.providerKey
      find(userId)
    } else {
      Future.successful(None)
    }
  }

  def find(id: String): Future[Option[User]] = {
    dataService.run(findQueryFor(id).result.map(_.headOption))
  }

  def findFromEvent(event: Event, team: Team): Future[Option[User]] = {
    event match {
      case slackEvent: SlackMessageEvent => dataService.linkedAccounts.find(LoginInfo(slackEvent.name, slackEvent.user), team.id).map { maybeLinked =>
        maybeLinked.map(_.user)
      }
      case _ => Future.successful(None)
    }
  }

  def createOnTeamWithId(teamId: String): User = User(IDs.next, teamId, None)
  def createOnTeam(team: Team): User = createOnTeamWithId(team.id)

  def createFor(teamId: String): Future[User] = save(createOnTeamWithId(teamId))

  def saveAction(user: User): DBIO[User] = {
    val query = findQueryFor(user.id)
    query.result.flatMap { result =>
      result.headOption.map { existing =>
        all.filter(_.id === user.id).update(user)
      }.getOrElse {
        all += user
      }.map { _ => user }
    }
  }

  def save(user: User): Future[User] = {
    dataService.run(saveAction(user))
  }

  def ensureUserForAction(loginInfo: LoginInfo, teamId: String): DBIO[User] = {
    dataService.linkedAccounts.findAction(loginInfo, teamId).flatMap { maybeLinkedAccount =>
      maybeLinkedAccount.map(DBIO.successful).getOrElse {
        saveAction(createOnTeamWithId(teamId)).flatMap { user =>
          dataService.linkedAccounts.saveAction(LinkedAccount(user, loginInfo, OffsetDateTime.now))
        }
      }.map(_.user)
    }
  }

  def ensureUserFor(loginInfo: LoginInfo, teamId: String): Future[User] = {
    dataService.run(ensureUserForAction(loginInfo, teamId))
  }

  def teamAccessForAction(user: User, maybeTargetTeamId: Option[String]): DBIO[UserTeamAccess] = {
    for {
      loggedInTeam <- dataService.teams.findAction(user.teamId).map(_.get)
      maybeSlackLinkedAccount <- dataService.linkedAccounts.maybeForSlackForAction(user)
      isAdmin <- maybeSlackLinkedAccount.map(dataService.linkedAccounts.isAdminAction).getOrElse(DBIO.successful(false))
      maybeTeam <- maybeTargetTeamId.map { targetTeamId =>
        if (targetTeamId != user.teamId && !isAdmin) {
          DBIO.successful(None)
        } else {
          dataService.teams.findAction(targetTeamId)
        }
      }.getOrElse {
        dataService.teams.findAction(user.teamId)
      }
    } yield UserTeamAccess(user, loggedInTeam, maybeTeam, maybeTeam.exists(t => t.id != user.teamId))
  }

  def teamAccessFor(user: User, maybeTargetTeamId: Option[String]): Future[UserTeamAccess] = {
    dataService.run(teamAccessForAction(user, maybeTargetTeamId))
  }

  def isAdmin(user: User): Future[Boolean] = {
    for {
      linkedAccounts <- dataService.linkedAccounts.allFor(user)
      isAdmins <- Future.sequence(linkedAccounts.map { acc =>
        dataService.linkedAccounts.isAdmin(acc)
      })
    } yield {
      isAdmins.contains(true)
    }
  }

  def maybeNameFor(user: User, event: SlackMessageEvent): Future[Option[String]] = {
    for {
      maybeSlackAccount <- dataService.linkedAccounts.maybeForSlackFor(user)
      maybeName <- maybeSlackAccount.map { acc =>
        event.client.getUserInfo(acc.loginInfo.providerKey).map(info => Some(info.name)).recover {
          case e: ApiError => None
        }
      }.getOrElse(Future.successful(None))
    } yield maybeName
  }

  def findForInvocationToken(tokenId: String): Future[Option[User]] = {
    for {
      maybeToken <- dataService.invocationTokens.findNotExpired(tokenId)
      maybeUser <- maybeToken.map { token =>
        find(token.userId)
      }.getOrElse {
        if (configuration.getOptional[String]("application.version").contains("Development")) {
          // in dev, if not found, we assume the tokenId is a user ID
          find(tokenId)
        } else {
          Future.successful(None)
        }
      }
    } yield maybeUser
  }

}
