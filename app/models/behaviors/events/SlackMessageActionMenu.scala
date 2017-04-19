package models.behaviors.events

import slack.models.{ActionField, ActionSelectOption}

case class SlackMessageActionMenu(name: String, text: String, options: Seq[SlackMessageActionMenuItem]) extends SlackMessageAction {
  lazy val selectOptions = options.map(ea => ActionSelectOption(ea.text, ea.value))
  lazy val actionField: ActionField = ActionField(name, text, `type` = "select", options = Some(selectOptions))
}

case class SlackMessageActionMenuItem(text: String, value: String)
