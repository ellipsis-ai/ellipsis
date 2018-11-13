package models.behaviors.events.slack

import models.behaviors.events.MessageUserData
import utils.Color

case class SlackMessageTextAttachmentGroup(
                                          text: String,
                                          maybeUserDataList: Option[Set[MessageUserData]],
                                          maybeTitle: Option[String] = None
                                        ) extends SlackMessageAttachmentGroup {

  val attachments: Seq[SlackMessageAttachment] = {
    Seq(SlackMessageAttachment(
      Some(text),
      maybeUserDataList,
      maybeTitle,
      None,
      Some(Color.BLUE_LIGHT)
    ))
  }
}
