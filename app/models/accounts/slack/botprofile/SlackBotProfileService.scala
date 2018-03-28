package models.accounts.slack.botprofile

import java.time.OffsetDateTime

import models.behaviors.BotResult
import models.behaviors.events.{EventType, SlackMessageEvent}
import models.team.Team
import services.caching.CacheService
import slick.dbio.DBIO
import utils.SlackChannels

import scala.concurrent.Future

trait SlackBotProfileService {

  def allProfiles: Future[Seq[SlackBotProfile]]

  def allForAction(team: Team): DBIO[Seq[SlackBotProfile]]

  def allFor(team: Team): Future[Seq[SlackBotProfile]]

  def allForSlackTeamId(slackTeamId: String): Future[Seq[SlackBotProfile]]

  def allSince(when: OffsetDateTime): Future[Seq[SlackBotProfile]]

  def ensure(userId: String, slackTeamId: String, slackTeamName: String, token: String): Future[SlackBotProfile]

  def channelsFor(botProfile: SlackBotProfile, cacheService: CacheService): SlackChannels

  def eventualMaybeEvent(slackTeamId: String, channelId: String, maybeUserId: Option[String], maybeOriginalEventType: Option[EventType]): Future[Option[SlackMessageEvent]]

  def maybeNameFor(slackTeamId: String): Future[Option[String]]

  def maybeNameFor(botProfile: SlackBotProfile): Future[Option[String]]

  def sendResultWithNewEvent(
    description: String,
    getEventualMaybeResult: SlackMessageEvent => Future[Option[BotResult]],
    slackTeamId: String,
    botProfile: SlackBotProfile,
    channelId: String,
    userId: String,
    originalMessageTs: String
  ): Future[Unit]
}
