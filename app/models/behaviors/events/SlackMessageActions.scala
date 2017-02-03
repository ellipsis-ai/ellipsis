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

  def attachments: Seq[Attachment] = {
    val size = actions.length
    val maxPerGroup = SlackMessageEvent.MAX_ACTIONS_PER_ATTACHMENT
    val groupSize = if (size % maxPerGroup == 1) { maxPerGroup - 1 } else { maxPerGroup }
    val maybeFormattedText = maybeText.map(SlackMessageFormatter.bodyTextFor)
    actions.grouped(groupSize).zipWithIndex.map { case(segment, index) =>
      Attachment(
        fallback = Some("This feature is unavailable on this platform."),
        actions = segment.map(_.actionField),
        callback_id = Some(id),
        color = maybeColor,
        mrkdwn_in = Seq("text"),
        text = if (index == 0) { maybeFormattedText } else { None },
        title = if (index == 0) { maybeTitle } else { None }
      )
    }.toSeq
  }

}

