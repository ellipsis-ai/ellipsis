package models.behaviors.events.ms_teams

import models.behaviors.events.{MessageAttachment, MessageUserData}
import services.ms_teams.apiModels.{AdaptiveCard, CardAction, ContentAttachment}

case class MSTeamsMessageAttachment(
                                   maybeText: Option[String],
                                   maybeUserDataList: Option[Set[MessageUserData]],
                                   maybeTitle: Option[String] = None,
                                   maybeTitleLink: Option[String] = None,
                                   maybeColor: Option[String] = None,
                                   maybeCallbackId: Option[String] = None,
                                   actions: Seq[CardAction] = Seq()
                                 ) extends MessageAttachment {
  val underlying = ContentAttachment(
    "application/vnd.microsoft.card.adaptive",
    AdaptiveCard(Seq(), actions)
  )
}
