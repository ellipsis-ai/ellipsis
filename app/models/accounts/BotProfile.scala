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

  def maybeContextFor(contextString: String): Option[BotContext] = {
    values.find(_.name == contextString)
  }
}

sealed trait BotContext extends BotContext.Value {
  val name: String
  val description: String
}

case object SlackContext extends BotContext {
  override val name: String = "slack"
  override val description: String = "Slack"
  override def toString: String = name
}

case object MSTeamsContext extends BotContext {
  override val name: String = "ms_teams"
  override val description: String = "Microsoft Teams"
  override def toString: String = name
}

case object MSAzureActiveDirectoryContext extends BotContext {
  override val name: String = "ms_aad"
  override val description: String = "Microsoft AAD"
  override def toString: String = name
}
