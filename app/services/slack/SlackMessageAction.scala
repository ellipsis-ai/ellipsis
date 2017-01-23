package services.slack

import slack.models.ActionField

case class SlackMessageAction(name: String, text: String, value: String) extends MessageAction {

  lazy val actionField: ActionField = ActionField(name, text, value = Some(value), `type` = "button")

}
