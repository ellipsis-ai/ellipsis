package models.behaviors.events

import models.SlackMessageFormatter
import slack.models.Attachment

case class SlackMessageActions(
                                id: String,
                                actions: Seq[SlackMessageAction],
                                maybeText: Option[String],
                                maybeColor: Option[String],
                                maybeTitle: Option[String] = None
                              ) extends MessageActions {

  type T = SlackMessageAction

  def attachmentSegments: Seq[Attachment] = {
    val size = actions.length
    val maxPerGroup = SlackMessageEvent.MAX_ACTIONS_PER_ATTACHMENT
    val groupSize = if (size % maxPerGroup == 1) { maxPerGroup - 1 } else { maxPerGroup }
    actions.grouped(groupSize).map { actionPart =>
      Attachment(
        fallback = Some("Buttons unavailable"),
        actions = actionPart.map(_.actionField),
        callback_id = Some(id),
        color = maybeColor
      )
    }.toSeq
  }

  lazy val attachments: Seq[Attachment] = {
    maybeText.map { unformatted =>
      val formatted = Some(SlackMessageFormatter.bodyTextFor(unformatted))
      Seq(Attachment(
        fallback = formatted,
        title = maybeTitle,
        text = formatted,
        color = maybeColor,
        mrkdwn_in = Seq("text")
      )) ++ attachmentSegments
    }.getOrElse {
      attachmentSegments.zipWithIndex.map { case(segment, index) =>
        if (index == 0) {
          segment.copy(title = maybeTitle)
        } else {
          segment
        }
      }
    }
  }

}

