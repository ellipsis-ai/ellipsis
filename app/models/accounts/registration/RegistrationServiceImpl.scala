package models.accounts.registration

import javax.inject.{Inject, Provider}

import models.organization.Organization
import models.team.Team
import services.DataService

import scala.concurrent.{ExecutionContext, Future}


class RegistrationServiceImpl @Inject() (
                                          dataServiceProvider: Provider[DataService],
                                          implicit val ec: ExecutionContext
                                        ) extends RegistrationService {

  def dataService = dataServiceProvider.get

  def registerNewTeam(teamName: String): Future[Team] = {
    dataService.teams.create(teamName)
  }
}
