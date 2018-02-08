package models.loggedevent

import utils.Enum

sealed trait CauseType {
  val name: String
  override def toString: String = name
}


object CauseType extends Enum[CauseType] {
  val values = List(
    TriggerMatchedInChat,
    ConversationResponse,
    BotMentioned,
    PrivateMessageToBot,
    ScheduledBehaviorRun,
    ScheduledMessageRun,
    ApiBehaviorRun,
    ApiMessageRun
  )
  def definitelyFind(name: String): CauseType = find(name).get
}

object TriggerMatchedInChat extends CauseType {
  val name: String = "trigger_matched_in_chat"
}

object ConversationResponse extends CauseType {
  val name: String = "conversation_response"
}

object BotMentioned extends CauseType {
  val name: String = "bot_mentioned"
}

object PrivateMessageToBot extends CauseType {
  val name: String = "private_message_to_bot"
}

object ScheduledBehaviorRun extends CauseType {
  val name: String = "scheduled_behavior_run"
}

object ScheduledMessageRun extends CauseType {
  val name: String = "scheduled_message_run"
}

object ApiBehaviorRun extends CauseType {
  val name: String = "api_behavior_run"
}

object ApiMessageRun extends CauseType {
  val name: String = "api_message_run"
}
