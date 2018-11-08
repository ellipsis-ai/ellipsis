package models.accounts.ms_teams.botprofile

import java.time.OffsetDateTime

import models.accounts.BotProfile
import models.behaviors.conversations.conversation.Conversation

case class MSTeamsBotProfile(
                              userId: String,
                              teamId: String,
                              teamIdForContext: String,
                              token: String,
                              expiresAt: OffsetDateTime,
                              refreshToken: String,
                              createdAt: OffsetDateTime,
                              allowShortcutMention: Boolean
                            ) extends BotProfile {

  val context: String = Conversation.MS_TEAMS_CONTEXT

}

object MSTeamsBotProfile {
  val ALLOW_SHORTCUT_MENTION_DEFAULT = false
}
