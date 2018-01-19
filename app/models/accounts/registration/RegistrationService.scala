package models.accounts.registration

import models.team.Team
import scala.concurrent.Future


trait RegistrationService {

  def registerNewTeam(teamName: String): Future[Team]

}
