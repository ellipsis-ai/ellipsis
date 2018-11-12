package models.accounts.ms_teams.botprofile

import java.time.OffsetDateTime

import models.accounts.{BotContext, BotProfile, MSTeamsContext}

case class MSTeamsBotProfile(
                              teamId: String,
                              tenantId: String,
                              createdAt: OffsetDateTime,
                              allowShortcutMention: Boolean
                            ) extends BotProfile {

  val context: BotContext = MSTeamsContext
  val teamIdForContext: String = tenantId

}

object MSTeamsBotProfile {
  val ALLOW_SHORTCUT_MENTION_DEFAULT = false
}
