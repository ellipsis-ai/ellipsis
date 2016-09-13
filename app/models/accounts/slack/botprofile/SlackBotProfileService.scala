package models.accounts.slack.botprofile

import models.team.Team
import org.joda.time.DateTime

import scala.concurrent.Future

trait SlackBotProfileService {

  def allProfiles: Future[Seq[SlackBotProfile]]

  def allFor(team: Team): Future[Seq[SlackBotProfile]]

  def allForSlackTeamId(slackTeamId: String): Future[Seq[SlackBotProfile]]

  def allSince(when: DateTime): Future[Seq[SlackBotProfile]]

  def ensure(userId: String, slackTeamId: String, slackTeamName: String, token: String): Future[SlackBotProfile]

}
