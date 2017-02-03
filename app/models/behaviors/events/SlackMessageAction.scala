package models.behaviors.events

import slack.models.ActionField

case class SlackMessageAction(name: String, text: String, value: String, style: Option[String] = None) extends MessageAction {

  lazy val actionField: ActionField = ActionField(name, text, value = Some(value), `type` = "button", style = style)

}
