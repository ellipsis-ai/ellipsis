package models.behaviors.events.ms_teams

import models.behaviors.events.{MessageActionsGroup, MessageUserData}
import utils.MSTeamsMessageSender

case class MSTeamsMessageActionsGroup(
                                     id: String,
                                     actions: Seq[MSTeamsMessageAction],
                                     maybeText: Option[String],
                                     maybeUserDataList: Option[Set[MessageUserData]],
                                     maybeColor: Option[String],
                                     maybeTitle: Option[String] = None,
                                     maybeTitleLink: Option[String] = None
                                   ) extends MSTeamsMessageAttachmentGroup with MessageActionsGroup {

  override type ActionType = MSTeamsMessageAction

  val attachments: Seq[MSTeamsMessageAttachment] = {
    val size = actions.length
    val maxPerGroup = MSTeamsMessageSender.MAX_ACTIONS_PER_ATTACHMENT
    val groupSize = if (size % maxPerGroup == 1) { maxPerGroup - 1 } else { maxPerGroup }
    val maybeCallbackId = Some(id)
    actions.grouped(groupSize).zipWithIndex.map { case(segment, index) =>
      val actions = segment.map(_.cardAction)
      if (index == 0) {
        MSTeamsMessageAttachment(
          maybeText,
          maybeUserDataList,
          maybeTitle,
          maybeTitleLink,
          maybeColor,
          maybeCallbackId,
          actions
        )
      } else {
        MSTeamsMessageAttachment(
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