package models.behaviors.events.slack

import models.behaviors.events.MessageAction
import services.slack.apiModels.InteractiveBlockElement

trait SlackMessageAction extends MessageAction {
  val actionField: InteractiveBlockElement
}
