package models.accounts

import java.time.OffsetDateTime

trait BotProfile {
  val context: String
  val userId: String
  val teamId: String
  val teamIdForContext: String
  val token: String
  val createdAt: OffsetDateTime
  val allowShortcutMention: Boolean
}
