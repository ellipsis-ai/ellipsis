package models.accounts.slack.botprofile

import org.joda.time.DateTime

case class SlackBotProfile(userId: String, teamId: String, slackTeamId: String, token: String, createdAt: DateTime)
