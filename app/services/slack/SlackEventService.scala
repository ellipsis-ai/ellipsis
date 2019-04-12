package services.slack

import json.SlackUserData
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.events.Event
import services.slack.apiModels.SlackUser
import slick.dbio.DBIO

import scala.concurrent.Future
import scala.util.Random

trait SlackEventService {

  val random = new Random()

  def onEvent(event: Event): Future[Unit]

  def clientFor(botProfile: SlackBotProfile): SlackApiClient

  def slackUserDataList(slackUserIds: Set[String], botProfile: SlackBotProfile): Future[Set[SlackUserData]]

  def maybeSlackUserDataForAction(slackUserId: String, client: SlackApiClient, onUserNotFound: SlackApiError => Option[SlackUser]): DBIO[Option[SlackUserData]]
  def maybeSlackUserDataFor(slackUserId: String, client: SlackApiClient, onUserNotFound: SlackApiError => Option[SlackUser]): Future[Option[SlackUserData]]

  def maybeSlackUserDataForAction(botProfile: SlackBotProfile): DBIO[Option[SlackUserData]]
  def maybeSlackUserDataFor(botProfile: SlackBotProfile): Future[Option[SlackUserData]]

  def maybeSlackUserDataForEmail(email: String, client: SlackApiClient): Future[Option[SlackUserData]]

  def isUserValidForBot(slackUserId: String, botProfile: SlackBotProfile, maybeEnterpriseId: Option[String]): Future[Boolean]

}
