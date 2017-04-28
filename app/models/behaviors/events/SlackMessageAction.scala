package models.behaviors.events

import slack.models.ActionField

trait SlackMessageAction extends MessageAction {
  val actionField: ActionField
}
