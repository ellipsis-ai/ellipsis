package models.accounts.user

import javax.inject.Inject

import com.google.inject.Provider
import com.mohiva.play.silhouette.api.LoginInfo
import models.accounts.linkedaccount.LinkedAccount
import models.bots.events.{MessageContext, SlackMessageContext}
import models.IDs
import models.team.Team
import org.joda.time.DateTime
import services.DataService
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UserServiceImpl @Inject() (dataServiceProvider: Provider[DataService]) extends UserService {

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

  def findFromMessageContext(context: MessageContext, team: Team): Future[Option[User]] = {
    context match {
      case mc: SlackMessageContext => dataService.linkedAccounts.find(LoginInfo(mc.name, mc.userIdForContext), team.id).map { maybeLinked =>
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
          dataService.linkedAccounts.save(LinkedAccount(user, loginInfo, DateTime.now))
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


}
