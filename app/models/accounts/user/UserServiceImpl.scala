package models.accounts.user

import java.time.OffsetDateTime
import javax.inject.Inject

import com.google.inject.Provider
import com.mohiva.play.silhouette.api.LoginInfo
import models.accounts.linkedaccount.LinkedAccount
import models.IDs
import models.team.Team
import services.DataService
import slack.api.ApiError
import drivers.SlickPostgresDriver.api._
import play.api.Configuration
import services.slack.{MessageEvent, SlackMessageEvent}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UserServiceImpl @Inject() (
                                  dataServiceProvider: Provider[DataService],
                                  configuration: Configuration
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

  def findFromMessageEvent(event: MessageEvent, team: Team): Future[Option[User]] = {
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

  def save(user: User): Future[User] = {
    val query = findQueryFor(user.id)
    val action = query.result.flatMap { result =>
      result.headOption.map { existing =>
        all.filter(_.id === user.id).update(user)
      }.getOrElse {
        all += user
      }.map { _ => user }
    }
    dataService.run(action)
  }

  def ensureUserFor(loginInfo: LoginInfo, teamId: String): Future[User] = {
    dataService.linkedAccounts.find(loginInfo, teamId).flatMap { maybeLinkedAccount =>
      maybeLinkedAccount.map(Future.successful).getOrElse {
        save(createOnTeamWithId(teamId)).flatMap { user =>
          dataService.linkedAccounts.save(LinkedAccount(user, loginInfo, OffsetDateTime.now))
        }
      }.map(_.user)
    }
  }

  def teamAccessFor(user: User, maybeTargetTeamId: Option[String]): Future[UserTeamAccess] = {
    for {
      loggedInTeam <- dataService.teams.find(user.teamId).map(_.get)
      maybeSlackLinkedAccount <- dataService.linkedAccounts.maybeForSlackFor(user)
      isAdmin <- maybeSlackLinkedAccount.map(dataService.linkedAccounts.isAdmin).getOrElse(Future.successful(false))
      maybeTeam <- maybeTargetTeamId.map { targetTeamId =>
        if (targetTeamId != user.teamId && !isAdmin) {
          Future.successful(None)
        } else {
          dataService.teams.find(targetTeamId)
        }
      }.getOrElse {
        dataService.teams.find(user.teamId)
      }
    } yield UserTeamAccess(user, loggedInTeam, maybeTeam, maybeTeam.exists(t => t.id != user.teamId))
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
        if (configuration.getString("application.version").contains("Development")) {
          // in dev, if not found, we assume the tokenId is a user ID
          find(tokenId)
        } else {
          Future.successful(None)
        }
      }
    } yield maybeUser
  }

}
