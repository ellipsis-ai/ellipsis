package models.team

import javax.inject.Inject

import com.google.inject.Provider
import models.accounts.linkedaccount.LinkedAccount
import models.IDs
import models.accounts.user.User
import play.api.Configuration
import services.DataService
import drivers.SlickPostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TeamServiceImpl @Inject() (
                                  dataServiceProvider: Provider[DataService],
                                  configuration: Configuration
                                ) extends TeamService {

  def dataService = dataServiceProvider.get

  import TeamQueries._

  def allTeams: Future[Seq[Team]] = {
    dataService.run(all.result)
  }

  def find(id: String): Future[Option[Team]] = {
    val action = findQueryFor(id).result.map(_.headOption)
    dataService.run(action)
  }

  def findByName(name: String): Future[Option[Team]] = {
    val action = findByNameQueryFor(name).result.map(_.headOption)
    dataService.run(action)
  }

  def find(id: String, user: User): Future[Option[Team]] = {
    for {
      maybeTeam <- find(id)
      canAccess <- maybeTeam.map { team =>
        dataService.users.canAccess(user, team)
      }.getOrElse(Future.successful(false))
    } yield if (canAccess) {
      maybeTeam
    } else {
      None
    }
  }

  def findForInvocationToken(tokenId: String): Future[Option[Team]] = {
    for {
      maybeUser <- dataService.users.findForInvocationToken(tokenId)
      maybeTeam <- maybeUser.map { user =>
        dataService.teams.find(user.teamId)
      }.getOrElse(Future.successful(None))
    } yield maybeTeam
  }

  def create(name: String): Future[Team] = save(Team(IDs.next, name))

  def setInitialNameFor(team: Team, name: String): Future[Team] = {
    if (team.maybeNonEmptyName.isEmpty) {
      save(team.copy(name = name))
    } else {
      Future.successful(team)
    }
  }

  def save(team: Team): Future[Team] = {
    val query = findQueryFor(team.id)
    val action = query.result.flatMap { result =>
      result.headOption.map { existing =>
        all.filter(_.id === team.id).update(team)
      }.getOrElse {
        all += team
      }.map { _ => team }
    }
    dataService.run(action)
  }

  def isAdmin(team: Team): Future[Boolean] = {
    dataService.slackBotProfiles.allFor(team).map { botProfiles =>
      botProfiles.exists(_.slackTeamId == LinkedAccount.ELLIPSIS_SLACK_TEAM_ID)
    }
  }
}
