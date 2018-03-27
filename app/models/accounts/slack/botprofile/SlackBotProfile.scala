package models.accounts.slack.botprofile

import java.time.OffsetDateTime

import models.behaviors.events.{MessageEvent, SlackMessage, SlackMessageEvent}

case class SlackBotProfile(userId: String, teamId: String, slackTeamId: String, token: String, createdAt: OffsetDateTime) {

  def includesBotMention(message: SlackMessage): Boolean = {
    SlackMessageEvent.mentionRegexFor(userId).findFirstMatchIn(message.originalText).nonEmpty ||
      MessageEvent.ellipsisRegex.findFirstMatchIn(message.originalText).nonEmpty
  }
}
