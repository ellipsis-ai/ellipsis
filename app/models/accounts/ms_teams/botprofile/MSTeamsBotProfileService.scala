package models.accounts.ms_teams.botprofile

import scala.concurrent.Future

trait MSTeamsBotProfileService {

  def find(tenantId: String): Future[Option[MSTeamsBotProfile]]

  def ensure(tenantId: String, teamName: String): Future[MSTeamsBotProfile]

}
