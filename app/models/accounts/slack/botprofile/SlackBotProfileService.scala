package models.accounts.slack.botprofile

import java.time.OffsetDateTime

import models.team.Team
import slack.api.SlackApiClient
import utils.SlackChannels

import scala.concurrent.Future

trait SlackBotProfileService {

  def allProfiles: Future[Seq[SlackBotProfile]]

  def allFor(team: Team): Future[Seq[SlackBotProfile]]

  def allForSlackTeamId(slackTeamId: String): Future[Seq[SlackBotProfile]]

  def allSince(when: OffsetDateTime): Future[Seq[SlackBotProfile]]

  def ensure(userId: String, slackTeamId: String, slackTeamName: String, token: String): Future[SlackBotProfile]

  def channelsFor(botProfile: SlackBotProfile): SlackChannels = {
    SlackChannels(SlackApiClient(botProfile.token))
  }

}
