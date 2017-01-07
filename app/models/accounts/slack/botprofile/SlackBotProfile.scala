package models.accounts.slack.botprofile

import java.time.ZonedDateTime

case class SlackBotProfile(userId: String, teamId: String, slackTeamId: String, token: String, createdAt: ZonedDateTime)
