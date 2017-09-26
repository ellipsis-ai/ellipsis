package models.behaviors.events

import slack.models.ActionField

trait SlackMessageAction {
  val actionField: ActionField
}
