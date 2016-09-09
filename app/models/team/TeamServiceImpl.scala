package models.team

import javax.inject.Inject

import com.google.inject.Provider
import models.accounts.SlackBotProfileQueries
import models.accounts.linkedaccount.LinkedAccount
import models.IDs
import models.accounts.user.User
import services.DataService
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TeamServiceImpl @Inject() (
                                  dataServiceProvider: Provider[DataService]
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

  def findForToken(tokenId: String): Future[Option[Team]] = {
    for {
      maybeToken <- dataService.invocationTokens.find(tokenId)
      maybeTeam <- maybeToken.map { token =>
        if (token.isExpired || token.isUsed) {
          Future.successful(None)
        } else {
          dataService.invocationTokens.use(token).flatMap { _ =>
            find(token.teamId)
          }
        }
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
    val action = for {
      botProfiles <- SlackBotProfileQueries.allFor(team)
    } yield {
      botProfiles.exists(_.slackTeamId == LinkedAccount.ELLIPSIS_SLACK_TEAM_ID)
    }
    dataService.run(action)
  }
}
