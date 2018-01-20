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

  def registerNewTeam(name: String): Future[Team] = {
    for {
      org <- dataService.organizations.create(name)
      team <- dataService.teams.create(name, org)
    } yield {
      team
    }

  }
}
