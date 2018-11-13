package models.behaviors.events.slack

import services.slack.apiModels.ActionField

case class SlackMessageActionButton(name: String, text: String, value: String, maybeStyle: Option[String] = None) extends SlackMessageAction {

  lazy val actionField: ActionField = ActionField(name, text, value = Some(value), `type` = "button", style = maybeStyle)
}
