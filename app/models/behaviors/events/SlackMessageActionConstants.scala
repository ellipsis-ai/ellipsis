package models.behaviors.events

import scala.util.matching.Regex

object SlackMessageActionConstants {
  val SHOW_BEHAVIOR_GROUP_HELP = "help_for_skill"
  val LIST_BEHAVIOR_GROUP_ACTIONS = "help_actions_for_skill"
  val SHOW_HELP_INDEX = "help_index"
  val CONFIRM_CONTINUE_CONVERSATION = "confirm_continue_conversation"
  val DONT_CONTINUE_CONVERSATION = "dont_continue_conversation"
  val STOP_CONVERSATION = "stop_conversation"
  val RUN_BEHAVIOR_VERSION = "run_behavior_version"
  val INPUT_CHOICE = "input_choice"
  val DATA_TYPE_CHOICE = "data_type_choice"
  val ACTION_CHOICES = "action_choices"
  val ACTION_CHOICE = "action_choice"
  val YES_NO_CHOICE = "yes_no_choice"
  val YES = "yes"
  val NO = "no"

  def callbackIdFor(kind: String, slackUserId: String, maybeConversationId: Option[String]): String = {
    val conversationId = maybeConversationId.getOrElse("")
    s"$kind/$slackUserId/$conversationId"
  }
  def callbackIdRegexFor(kind: String): Regex = {
    raw"""^$kind\/([^\/]+)\/([^\/]+)$$""".r
  }

  def maybeUserIdForCallbackId(kind: String, callbackId: String): Option[String] = {
    callbackIdRegexFor(kind).findFirstMatchIn(callbackId).flatMap(_.subgroups.headOption)
  }
  def maybeConversationIdForCallbackId(kind: String, callbackId: String): Option[String] = {
    callbackIdRegexFor(kind).findFirstMatchIn(callbackId).flatMap(_.subgroups.tail.headOption)
  }

  def dataTypeChoiceCallbackIdFor(slackUserId: String, maybeConversationId: Option[String]): String = {
    callbackIdFor(DATA_TYPE_CHOICE, slackUserId, maybeConversationId)
  }

  def yesNoCallbackIdFor(slackUserId: String, maybeConversationId: Option[String]): String = {
    callbackIdFor(YES_NO_CHOICE, slackUserId, maybeConversationId)
  }

  def continueConversationCallbackIdFor(slackUserId: String, maybeConversationId: Option[String]): String = {
    callbackIdFor(CONFIRM_CONTINUE_CONVERSATION, slackUserId, maybeConversationId)
  }

}
