package models.behaviors.events.ms_teams

import json.UserData
import models.behaviors.events.MessageAttachment
import services.ms_teams.apiModels.{AdaptiveCard, Attachment, CardElement, TextBlock}
import utils.Color

case class MSTeamsMessageAttachment(
                                     maybeText: Option[String] = None,
                                     maybeUserDataList: Option[Set[UserData]] = None,
                                     maybeTitle: Option[String] = None,
                                     maybeTitleLink: Option[String] = None,
                                     maybeColor: Option[Color] = None,
                                     maybeCallbackId: Option[String] = None,
                                     actions: Seq[MSTeamsMessageAction] = Seq()
                                 ) extends MessageAttachment {

  val bodyElements: Seq[CardElement] = maybeText.map(t => TextBlock(t)).toSeq ++ actions.flatMap(_.bodyElements)
  val actionElements: Seq[CardElement] = actions.flatMap(_.actionElements)

  val underlying = Attachment(
    "application/vnd.microsoft.card.adaptive",
    Some(AdaptiveCard(bodyElements, actionElements)),
    contentUrl = None,
    name = None
  )
}
