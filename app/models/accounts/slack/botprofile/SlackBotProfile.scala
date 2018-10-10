package models.accounts.slack.botprofile

import java.time.OffsetDateTime

import models.behaviors.events.{MessageEvent, SlackMessage, SlackMessageEvent}

case class SlackBotProfile(
                            userId: String,
                            teamId: String,
                            maybeSlackEnterpriseId: Option[String],
                            slackTeamId: String,
                            token: String,
                            createdAt: OffsetDateTime,
                            allowShortcutMention: Boolean
                          ) {

  def includesBotMention(message: SlackMessage): Boolean = {
    SlackMessageEvent.mentionRegexFor(userId).findFirstMatchIn(message.originalText).nonEmpty ||
      (allowShortcutMention && MessageEvent.ellipsisShortcutMentionRegex.findFirstMatchIn(message.originalText).nonEmpty)
  }

  val botDMDeepLink: String = s"slack://user?team=${slackTeamId}&id=${userId}"
}

object SlackBotProfile {
  val ALLOW_SHORTCUT_MENTION_DEFAULT = false
}
