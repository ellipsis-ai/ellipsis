package models.behaviors.events.slack

import models.IDs
import models.behaviors.events.{MessageMenu, MessageMenuItem}
import services.slack.apiModels.{ActionField, ActionSelectOption, PlainText, SelectElementOption, StaticSelectElement}

case class SlackMessageMenu(name: String, text: String, options: Seq[SlackMessageMenuItem]) extends MessageMenu with SlackMessageAction {
  lazy val selectOptions = options.map(ea => SelectElementOption(PlainText(ea.text), ea.value))
  lazy val actionField = StaticSelectElement(PlainText(text), name + IDs.next, selectOptions)
}

case class SlackMessageMenuItem(text: String, value: String) extends MessageMenuItem
