package models.behaviors.events

import slack.models.Attachment

case class SlackMessageActions(
                                id: String,
                                actions: Seq[SlackMessageAction],
                                maybeText: Option[String],
                                maybeColor: Option[String]
                              ) extends MessageActions {

  type T = SlackMessageAction

  lazy val attachments: Seq[Attachment] = {
    actions.grouped(SlackMessageEvent.MAX_ACTIONS_PER_ATTACHMENT).zipWithIndex.map { case(actionPart, index) => {
      Attachment(
        actions = actionPart.map(_.actionField),
        text = if (index == 0) { maybeText } else { None },
        callback_id = Some(id),
        color = maybeColor
      )
    }}.toList
  }

}

