package models.loggedevent

import utils.Enum

sealed trait LoggedEventType {
  val name: String
  override def toString: String = name
}


object LoggedEventType extends Enum[LoggedEventType] {
  val values = List(BotMessageSent, TriggerMatched)
  def definitelyFind(name: String): LoggedEventType = find(name).getOrElse(BotMessageSent)
}

object BotMessageSent extends LoggedEventType {
  val name: String = "bot_message_sent"
}

object TriggerMatched extends LoggedEventType {
  val name: String = "trigger_matched"
}

object ScheduledRun extends LoggedEventType {
  val name: String = "scheduled_run"
}
