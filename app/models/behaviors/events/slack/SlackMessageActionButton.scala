package models.behaviors.events.slack

import models.IDs
import models.behaviors.events.MessageActionButton
import services.slack.apiModels.{ButtonElement, ButtonStyle, PlainText}

case class SlackMessageActionButton(
                                     name: String,
                                     text: String,
                                     value: String,
                                     maybeStyle: Option[ButtonStyle] = None
                                   ) extends SlackMessageAction with MessageActionButton {

  lazy val actionField = ButtonElement(PlainText(text), name + IDs.next, url = None, value = Some(value), style = maybeStyle, confirm = None)
}
