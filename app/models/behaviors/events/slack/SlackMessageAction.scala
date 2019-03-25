package models.behaviors.events.slack

import models.behaviors.events.MessageAction
import services.slack.apiModels.ActionField

trait SlackMessageAction extends MessageAction {
  val actionField: ActionField
}
