package models.accounts.ms_teams.botprofile

import scala.concurrent.Future

trait MSTeamsBotProfileService {

  def ensure(tenantId: String, teamName: String): Future[MSTeamsBotProfile]

}
