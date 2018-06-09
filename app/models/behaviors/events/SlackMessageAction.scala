package models.behaviors.events

import services.slack.apiModels.ActionField

trait SlackMessageAction {
  val actionField: ActionField
}
