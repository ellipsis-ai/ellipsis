package models.team

import java.time.{OffsetDateTime, ZoneId}
import javax.inject.Inject

import com.google.inject.Provider
import models.accounts.linkedaccount.LinkedAccount
import models.IDs
import models.accounts.user.User
import play.api.Configuration
import services.DataService
import drivers.SlickPostgresDriver.api._
import models.organization.Organization

import scala.concurrent.{ExecutionContext, Future}

class TeamServiceImpl @Inject() (
                                  val dataServiceProvider: Provider[DataService],
                                  configuration: Configuration,
                                  implicit val ec: ExecutionContext
                                ) extends TeamService {

  def dataService = dataServiceProvider.get

  import TeamQueries._

  def allTeams: Future[Seq[Team]] = {
    dataService.run(all.result)
  }

  def allCount: Future[Int] = {
    dataService.run(all.length.result)
  }

  def allTeamsPaged(page: Int, perPage: Int): Future[Seq[Team]] = {
    dataService.run(allPagedQuery((page - 1) * perPage, perPage).result)
  }

  def allTeamsWithoutOrg: Future[Seq[Team]] = {
    dataService.run(withoutOrg.result)
  }

  def allTeamsFor(organization: Organization): Future[Seq[Team]] = {
    dataService.run(withOrganizationId(organization.id).result)
  }

  def findAction(id: String): DBIO[Option[Team]] = {
    findQueryFor(id).result.map(_.headOption)
  }

  def find(id: String): Future[Option[Team]] = {
    dataService.run(findAction(id))
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

  def create(name: String): Future[Team] = save(Team(name))

  def create(name: String, organization: Organization): Future[Team] = {
    save(Team(IDs.next, name, None, Some(organization.id), OffsetDateTime.now))
  }

  def createAction(name: String, organization: Organization): DBIO[Team] = {
    saveAction(Team(IDs.next, name, None, Some(organization.id), OffsetDateTime.now))
  }

  def setNameFor(team: Team, name: String): Future[Team] = {
    save(team.copy(name = name))
  }

  def setTimeZoneFor(team: Team, tz: ZoneId): Future[Team] = {
    save(team.copy(maybeTimeZone = Some(tz)))
  }

  def setOrganizationIdFor(team: Team, organizationId: Option[String]): Future[Team] = {
    save(team.copy(maybeOrganizationId = organizationId))
  }

  def save(team: Team): Future[Team] = {
    dataService.run(saveAction(team))
  }

  def isAdmin(team: Team): Future[Boolean] = {
    dataService.slackBotProfiles.allFor(team).map { botProfiles =>
      botProfiles.exists(_.slackTeamId == LinkedAccount.ELLIPSIS_SLACK_TEAM_ID)
    }
  }

  private def saveAction(team: Team): DBIO[Team] = {
    findQueryFor(team.id).result.flatMap { result =>
      result.headOption.map { existing =>
        all.filter(_.id === team.id).update(team)
      }.getOrElse {
        all += team
      }.map { _ => team }
    }
  }
}
