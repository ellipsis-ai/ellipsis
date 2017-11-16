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

  def inputChoiceCallbackIdFor(slackUserId: String): String = INPUT_CHOICE ++ "-" ++ slackUserId
  val userIdForCallbackIdRegex = raw"""^$INPUT_CHOICE\-(\S+)$$""".r
  def maybeUserIdForCallbackId(callbackId: String): Option[String] = userIdForCallbackIdRegex.findFirstMatchIn(callbackId).flatMap(_.subgroups.headOption)
}
