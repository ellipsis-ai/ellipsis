package services.slack

import json.SlackUserData
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.events.SlackMessageEvent
import services.slack.apiModels.SlackUser

import scala.concurrent.Future
import scala.util.Random

trait SlackEventService {

  val random = new Random()

  def onEvent(event: SlackMessageEvent): Future[Unit]

  def clientFor(botProfile: SlackBotProfile): SlackApiClient

  def slackUserDataList(slackUserIds: Set[String], botProfile: SlackBotProfile): Future[Set[SlackUserData]]

  def maybeSlackUserDataFor(slackUserId: String, slackTeamId: String, client: SlackApiClient, onUserNotFound: SlackApiError => Option[SlackUser]): Future[Option[SlackUserData]]

  def maybeSlackUserDataFor(botProfile: SlackBotProfile): Future[Option[SlackUserData]]

  def maybeSlackUserDataForEmail(email: String, client: SlackApiClient): Future[Option[SlackUserData]]

}
