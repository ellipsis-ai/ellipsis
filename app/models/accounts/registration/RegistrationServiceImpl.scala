package models.accounts.registration


import drivers.SlickPostgresDriver.api._
import javax.inject.Inject
import models.team.Team
import services.DataService

import scala.concurrent.ExecutionContext

class RegistrationServiceImpl @Inject() (
                                          val dataService: DataService,
                                          implicit val ec: ExecutionContext
                                        ) extends RegistrationService {

  def registerNewTeamAction(name: String): DBIO[Team] = {
    (for {
      org <- dataService.organizations.createAction(name)
      team <- dataService.teams.createAction(name, org)
    } yield team).transactionally
  }

}


