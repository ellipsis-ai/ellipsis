package models.team

import javax.inject.Inject

import com.google.inject.Provider
import models.{IDs, InvocationToken}
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
    val action = for {
      maybeToken <- InvocationToken.find(tokenId)
      maybeTeam <- maybeToken.map { token =>
        if (token.isExpired || token.isUsed) {
          DBIO.successful(None)
        } else {
          InvocationToken.use(token).flatMap { _ =>
            DBIO.from(find(token.teamId))
          }
        }
      }.getOrElse(DBIO.successful(None))
    } yield maybeTeam
    dataService.run(action)
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
}
