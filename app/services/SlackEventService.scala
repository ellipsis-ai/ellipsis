package services

import json.SlackUserData
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.events.SlackMessageEvent
import slack.api.{ApiError, SlackApiClient}

import scala.concurrent.Future
import scala.util.Random

trait SlackEventService {

  val random = new Random()

  def onEvent(event: SlackMessageEvent): Future[Unit]

  def clientFor(botProfile: SlackBotProfile): SlackApiClient

  def slackUserDataList(slackUserIds: Set[String], botProfile: SlackBotProfile): Future[Set[SlackUserData]]

  def maybeSlackUserDataFor(slackUserId: String, slackTeamId: String, client: SlackApiClient, onUserNotFound: (ApiError) => Option[slack.models.User]): Future[Option[SlackUserData]]

  def maybeSlackUserDataFor(botProfile: SlackBotProfile): Future[Option[SlackUserData]]

}
