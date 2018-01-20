package models.accounts.registration

import javax.inject.{Inject, Provider}

import models.billing.ChargebeeService
import services.DataService
import models.organization.Organization
import models.team.Team

import scala.concurrent.{ExecutionContext, Future}


class RegistrationServiceImpl @Inject() (
                                          val dataService: DataService,
                                          implicit val ec: ExecutionContext
                                        ) extends RegistrationService {

  def registerNewTeam(name: String): Future[Team] = {
    for {
      org <- dataService.organizations.create(name)
      team <- dataService.teams.create(name, org)
    } yield {
      team
    }
  }

}


