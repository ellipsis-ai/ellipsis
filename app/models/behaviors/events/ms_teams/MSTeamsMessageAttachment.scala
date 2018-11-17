package models.behaviors.events.ms_teams

import models.behaviors.events.{MessageAttachment, MessageUserData}
import services.ms_teams.apiModels.{AdaptiveCard, ContentAttachment}

case class MSTeamsMessageAttachment(
                                   maybeText: Option[String] = None,
                                   maybeUserDataList: Option[Set[MessageUserData]] = None,
                                   maybeTitle: Option[String] = None,
                                   maybeTitleLink: Option[String] = None,
                                   maybeColor: Option[String] = None,
                                   maybeCallbackId: Option[String] = None,
                                   actions: Seq[MSTeamsMessageAction] = Seq()
                                 ) extends MessageAttachment {
  val underlying = ContentAttachment(
    "application/vnd.microsoft.card.adaptive",
    AdaptiveCard(Seq(), actions.map(_.cardAction))
  )
}
