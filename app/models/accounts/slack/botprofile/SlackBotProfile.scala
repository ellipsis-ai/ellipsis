package models.accounts.slack.botprofile

import org.joda.time.LocalDateTime

case class SlackBotProfile(userId: String, teamId: String, slackTeamId: String, token: String, createdAt: LocalDateTime)
