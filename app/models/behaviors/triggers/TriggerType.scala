package models.behaviors.triggers

import models.behaviors.events.{Event, MessageEvent, SlackReactionAddedEvent, SlashCommandEvent}
import utils.Enum

object TriggerType extends Enum[TriggerType] {
  val values = List(MessageSent, ReactionAdded)
  def definitelyFind(name: String): TriggerType = find(name).getOrElse(MessageSent)
  def definitelyFind(maybeName: Option[String]): TriggerType = maybeName.map(definitelyFind).getOrElse(MessageSent)
}

sealed trait TriggerType extends TriggerType.Value {
  val displayString: String
  def matches(trigger: Trigger, event: Event): Boolean
}

case object MessageSent extends TriggerType {
  val displayString = "Message sent"
  def matches(trigger: Trigger, event: Event): Boolean = {
    event match {
      case e: MessageEvent => trigger.matches(e.relevantMessageText, e.includesBotMention)
      case e: SlashCommandEvent => trigger.matches(e.messageText, includesBotMention = true)
      case _ => false
    }
  }
}

case object ReactionAdded extends TriggerType {
  val displayString = "Reaction added"
  def matches(trigger: Trigger, event: Event): Boolean = {
    event match {
      case e: SlackReactionAddedEvent => trigger.matches(e.reaction, includesBotMention = true)
      case _ => false
    }
  }
}
