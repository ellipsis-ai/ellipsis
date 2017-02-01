package models.behaviors.events

import models.SlackMessageFormatter
import slack.models.Attachment

case class SlackMessageActions(
                                id: String,
                                actions: Seq[SlackMessageAction],
                                maybeText: Option[String],
                                maybeColor: Option[String]
                              ) extends MessageActions {

  type T = SlackMessageAction

  def attachmentSegments: Seq[Attachment] = actions.grouped(SlackMessageEvent.MAX_ACTIONS_PER_ATTACHMENT).map { actionPart =>
    Attachment(
      actions = actionPart.map(_.actionField),
      callback_id = Some(id),
      color = maybeColor
    )
  }.toSeq

  lazy val attachments: Seq[Attachment] = {
    maybeText.map { unformatted =>
      Seq(Attachment(
        text = Some(SlackMessageFormatter.bodyTextFor(unformatted)),
        color = maybeColor,
        mrkdwn_in = Seq("text")
      )) ++ attachmentSegments
    }.getOrElse(attachmentSegments)
  }

}

