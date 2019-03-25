package services.ms_teams

import models.accounts.ms_teams.botprofile.MSTeamsBotProfile
import models.behaviors.events.Event
import services.ms_teams.apiModels.Application

import scala.concurrent.Future
import scala.util.Random

trait MSTeamsEventService {

  val random = new Random()

  def onEvent(event: Event): Future[Unit]

  def clientFor(botProfile: MSTeamsBotProfile): MSTeamsApiClient

  def maybeApplicationDataFor(botProfile: MSTeamsBotProfile): Future[Option[Application]]

}
