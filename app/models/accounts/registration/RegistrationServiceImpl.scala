package models.accounts.registration


import javax.inject.Inject

import drivers.SlickPostgresDriver.api._
import models.team.Team
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

class RegistrationServiceImpl @Inject() (
                                          val dataService: DataService,
                                          implicit val ec: ExecutionContext
                                        ) extends RegistrationService {

  def registerNewTeam(name: String): Future[Team] = {
    val action = for {
      org <- dataService.organizations.createAction(name)
      team <- dataService.teams.createAction(name, org)
    } yield team
    dataService.run(action.transactionally)
  }

}


