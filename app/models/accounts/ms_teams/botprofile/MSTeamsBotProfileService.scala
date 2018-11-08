package models.accounts.ms_teams.botprofile

import java.time.OffsetDateTime

import scala.concurrent.Future

trait MSTeamsBotProfileService {

  def ensure(
              userId: String,
              slackTeamId: String,
              slackTeamName: String,
              token: String,
              expiresAt: OffsetDateTime,
              refreshToken: String
            ): Future[MSTeamsBotProfile]

}
