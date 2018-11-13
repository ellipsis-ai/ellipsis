package models.behaviors.events.slack

import services.slack.apiModels.ActionField

trait SlackMessageAction {
  val actionField: ActionField
}
