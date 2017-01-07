package models.accounts.slack.botprofile

import java.time.OffsetDateTime

case class SlackBotProfile(userId: String, teamId: String, slackTeamId: String, token: String, createdAt: OffsetDateTime)
