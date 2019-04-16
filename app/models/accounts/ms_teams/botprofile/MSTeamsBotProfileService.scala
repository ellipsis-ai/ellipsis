package models.accounts.ms_teams.botprofile

import models.behaviors.BotResult
import models.behaviors.events.EventType
import models.behaviors.events.ms_teams.MSTeamsMessageEvent
import services.ms_teams.apiModels.ActivityInfo
import slick.dbio.DBIO

import scala.concurrent.Future

trait MSTeamsBotProfileService {

  def find(tenantId: String): Future[Option[MSTeamsBotProfile]]

  def allForAction(teamId: String): DBIO[Seq[MSTeamsBotProfile]]
  def allFor(teamId: String): Future[Seq[MSTeamsBotProfile]]

  def ensure(tenantId: String, teamName: String): Future[MSTeamsBotProfile]

  def sendResultWithNewEvent(
                              description: String,
                              maybeOriginalEventType: Option[EventType],
                              getEventualMaybeResult: MSTeamsMessageEvent => Future[Option[BotResult]],
                              botProfile: MSTeamsBotProfile,
                              info: ActivityInfo,
                              channelId: String,
                              userId: String,
                              originalMessageTs: String,
                              maybeThreadTs: Option[String],
                              isEphemeral: Boolean,
                              beQuiet: Boolean
                            ): Future[Option[String]]

  def maybeNameFor(botProfile: MSTeamsBotProfile): Future[Option[String]]
}
