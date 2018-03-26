package models.behaviors.events

object SlackMessageActionConstants {
  val SHOW_BEHAVIOR_GROUP_HELP = "help_for_skill"
  val LIST_BEHAVIOR_GROUP_ACTIONS = "help_actions_for_skill"
  val SHOW_HELP_INDEX = "help_index"
  val CONFIRM_CONTINUE_CONVERSATION = "confirm_continue_conversation"
  val DONT_CONTINUE_CONVERSATION = "dont_continue_conversation"
  val STOP_CONVERSATION = "stop_conversation"
  val RUN_BEHAVIOR_VERSION = "run_behavior_version"
  val INPUT_CHOICE = "input_choice"
  val ACTION_CHOICES = "action_choices"
  val ACTION_CHOICE = "action_choice"
  val YES_NO_CHOICE = "yes_no_choice"
  val YES = "yes"
  val NO = "no"

  def inputChoiceCallbackIdFor(slackUserId: String, maybeConversationId: Option[String]): String = {
    val conversationId = maybeConversationId.getOrElse("")
    s"$INPUT_CHOICE/$slackUserId/$conversationId"
  }
  val inputChoiceCallbackIdRegex = raw"""^$INPUT_CHOICE\/([^\/]+)\/([^\/]+)$$""".r
  def maybeUserIdForCallbackId(callbackId: String): Option[String] = {
    inputChoiceCallbackIdRegex.findFirstMatchIn(callbackId).flatMap(_.subgroups.headOption)
  }
  def maybeConversationIdForCallbackId(callbackId: String): Option[String] = {
    inputChoiceCallbackIdRegex.findFirstMatchIn(callbackId).flatMap(_.subgroups.tail.headOption)
  }
}
