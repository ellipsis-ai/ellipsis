package models.behaviors.events

import models.behaviors.MessageUserData
import utils.SlackMessageSender

case class SlackMessageActionsGroup(
                                     id: String,
                                     actions: Seq[SlackMessageAction],
                                     maybeText: Option[String],
                                     maybeUserDataList: Option[Set[MessageUserData]],
                                     maybeColor: Option[String],
                                     maybeTitle: Option[String] = None,
                                     maybeTitleLink: Option[String] = None
                              ) extends SlackMessageAttachmentGroup {

  val attachments: Seq[SlackMessageAttachment] = {
    val size = actions.length
    val maxPerGroup = SlackMessageSender.MAX_ACTIONS_PER_ATTACHMENT
    val groupSize = if (size % maxPerGroup == 1) { maxPerGroup - 1 } else { maxPerGroup }
    val maybeCallbackId = Some(id)
    actions.grouped(groupSize).zipWithIndex.map { case(segment, index) =>
      val actions = segment.map(_.actionField)
      if (index == 0) {
        SlackMessageAttachment(
          maybeText,
          maybeUserDataList,
          maybeTitle,
          maybeTitleLink,
          maybeColor,
          maybeCallbackId,
          actions
        )
      } else {
        SlackMessageAttachment(
          None,
          None,
          None,
          None,
          maybeColor,
          maybeCallbackId,
          actions
        )
      }
    }.toSeq
  }

}

