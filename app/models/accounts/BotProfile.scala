package models.accounts

import java.time.OffsetDateTime
import utils.Enum

trait BotProfile {
  val context: BotContext
  val teamId: String
  val teamIdForContext: String
  val createdAt: OffsetDateTime
  val allowShortcutMention: Boolean
  val supportsSharedChannels: Boolean
}

object BotContext extends Enum[BotContext] {
  val values = List(SlackContext, MSTeamsContext)
}

sealed trait BotContext extends BotContext.Value

case object SlackContext extends BotContext {
  override def toString: String = "slack"
}

case object MSTeamsContext extends BotContext {
  override def toString: String = "ms_teams"
}
