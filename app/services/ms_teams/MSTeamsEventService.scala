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

//  def slackUserDataList(slackUserIds: Set[String], botProfile: SlackBotProfile): Future[Set[SlackUserData]]
//
//  def maybeSlackUserDataFor(slackUserId: String, client: SlackApiClient, onUserNotFound: SlackApiError => Option[SlackUser]): Future[Option[SlackUserData]]
//
//  def maybeSlackUserDataForEmail(email: String, client: SlackApiClient): Future[Option[SlackUserData]]
//
//  def isUserValidForBot(slackUserId: String, botProfile: SlackBotProfile, maybeEnterpriseId: Option[String]): Future[Boolean]

}
