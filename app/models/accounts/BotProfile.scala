package models.accounts

import java.time.OffsetDateTime

trait BotProfile {
  val context: String
  val teamId: String
  val teamIdForContext: String
  val createdAt: OffsetDateTime
  val allowShortcutMention: Boolean
}
