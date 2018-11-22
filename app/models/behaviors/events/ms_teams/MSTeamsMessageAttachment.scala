package models.behaviors.events.ms_teams

import models.behaviors.events.{MessageAttachment, MessageUserData}
import services.ms_teams.apiModels.{AdaptiveCard, CardElement, ContentAttachment, TextBlock}

case class MSTeamsMessageAttachment(
                                   maybeText: Option[String] = None,
                                   maybeUserDataList: Option[Set[MessageUserData]] = None,
                                   maybeTitle: Option[String] = None,
                                   maybeTitleLink: Option[String] = None,
                                   maybeColor: Option[String] = None,
                                   maybeCallbackId: Option[String] = None,
                                   actions: Seq[MSTeamsMessageAction] = Seq()
                                 ) extends MessageAttachment {

  val bodyElements: Seq[CardElement] = maybeText.map(t => TextBlock(t)).toSeq ++ actions.flatMap(_.bodyElements)
  val actionElements: Seq[CardElement] = actions.flatMap(_.actionElements)

  val underlying = ContentAttachment(
    "application/vnd.microsoft.card.adaptive",
    AdaptiveCard(bodyElements, actionElements),
    contentUrl = None,
    name = None
  )
}
