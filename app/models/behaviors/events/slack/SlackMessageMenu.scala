package models.behaviors.events.slack

import models.behaviors.events.{MessageMenu, MessageMenuItem}
import services.slack.apiModels.{ActionField, ActionSelectOption}

case class SlackMessageMenu(name: String, text: String, options: Seq[SlackMessageMenuItem]) extends MessageMenu with SlackMessageAction {
  lazy val selectOptions = options.map(ea => ActionSelectOption(ea.text, ea.value))
  lazy val actionField: ActionField = ActionField(name, text, `type` = "select", options = Some(selectOptions))
}

case class SlackMessageMenuItem(text: String, value: String) extends MessageMenuItem
