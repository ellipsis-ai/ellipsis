package models.accounts.registration


import models.team.Team
import slick.dbio.DBIO


trait RegistrationService {

  def registerNewTeamAction(name: String): DBIO[Team]

}
