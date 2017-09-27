package models.behaviors.events

import utils.Color

case class SlackMessageTextAttachments(
                                          text: String,
                                          maybeTitle: Option[String] = None
                                        ) extends SlackMessageAttachments {

  val attachments: Seq[SlackMessageAttachment] = {
    Seq(SlackMessageAttachment(
      Some(text),
      maybeTitle,
      None,
      Some(Color.BLUE_LIGHT)
    ))
  }
}
