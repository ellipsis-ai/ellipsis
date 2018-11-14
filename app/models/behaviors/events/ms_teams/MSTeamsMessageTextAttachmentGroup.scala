package models.behaviors.events.ms_teams

import models.behaviors.events.MessageUserData
import utils.Color

case class MSTeamsMessageTextAttachmentGroup(
                                            text: String,
                                            maybeUserDataList: Option[Set[MessageUserData]],
                                            maybeTitle: Option[String] = None
                                          ) extends MSTeamsMessageAttachmentGroup {

  val attachments: Seq[MSTeamsMessageAttachment] = {
    Seq(MSTeamsMessageAttachment(
      Some(text),
      maybeUserDataList,
      maybeTitle,
      None,
      Some(Color.BLUE_LIGHT)
    ))
  }
}
