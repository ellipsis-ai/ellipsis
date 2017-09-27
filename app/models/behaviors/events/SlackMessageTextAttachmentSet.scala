package models.behaviors.events

import utils.Color

case class SlackMessageTextAttachmentSet(
                                          text: String,
                                          maybeTitle: Option[String] = None
                                        ) extends SlackMessageAttachmentSet {

  val attachments: Seq[SlackMessageAttachment] = {
    Seq(SlackMessageAttachment(
      Some(text),
      maybeTitle,
      None,
      Some(Color.BLUE_LIGHT)
    ))
  }
}
