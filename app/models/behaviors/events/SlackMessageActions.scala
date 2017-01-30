package models.behaviors.events

import slack.models.Attachment

case class SlackMessageActions(
                                id: String,
                                actions: Seq[SlackMessageAction],
                                maybeText: Option[String],
                                maybeColor: Option[String]
                              ) extends MessageActions {

  type T = SlackMessageAction

  lazy val attachment: Attachment = {
    Attachment(
      actions = actions.map(_.actionField),
      text = maybeText,
      callback_id = Some(id),
      color = maybeColor
    )
  }

}
