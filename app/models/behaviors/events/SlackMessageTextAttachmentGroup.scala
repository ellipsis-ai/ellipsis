package models.behaviors.events

import utils.Color

case class SlackMessageTextAttachmentGroup(
                                          text: String,
                                          maybeTitle: Option[String] = None
                                        ) extends SlackMessageAttachmentGroup {

  val attachments: Seq[SlackMessageAttachment] = {
    Seq(SlackMessageAttachment(
      Some(text),
      maybeTitle,
      None,
      Some(Color.BLUE_LIGHT)
    ))
  }
}
