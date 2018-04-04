package models.behaviors.events

import json.SlackUserData
import utils.Color

case class SlackMessageTextAttachmentGroup(
                                          text: String,
                                          maybeSlackUserList: Option[Set[SlackUserData]],
                                          maybeTitle: Option[String] = None
                                        ) extends SlackMessageAttachmentGroup {

  val attachments: Seq[SlackMessageAttachment] = {
    Seq(SlackMessageAttachment(
      Some(text),
      maybeSlackUserList,
      maybeTitle,
      None,
      Some(Color.BLUE_LIGHT)
    ))
  }
}
