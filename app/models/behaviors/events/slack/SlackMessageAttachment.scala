package models.behaviors.events.slack

import json.UserData
import models.SlackMessageFormatter
import models.behaviors.events.MessageAttachment
import services.slack.apiModels.Attachment

case class SlackMessageAttachment(
                                   maybeText: Option[String] = None,
                                   maybeUserDataList: Option[Set[UserData]] = None,
                                   maybeTitle: Option[String] = None,
                                   maybeTitleLink: Option[String] = None,
                                   maybeColor: Option[String] = None,
                                   maybeCallbackId: Option[String] = None,
                                   actions: Seq[SlackMessageAction] = Seq()
                                 ) extends MessageAttachment {
  val underlying = Attachment(
    fallback = Some("This feature is unavailable on this platform."),
    callback_id = maybeCallbackId,
    color = maybeColor,
    pretext = None,
    author_name = None,
    author_link = None,
    author_icon = None,
    title = maybeTitle,
    title_link = maybeTitleLink,
    text = maybeText.map(text => SlackMessageFormatter.bodyTextFor(text, maybeUserDataList.getOrElse(Set.empty[UserData]))),
    fields = Seq(),
    image_url = None,
    thumb_url = None,
    actions = actions.map(_.actionField),
    mrkdwn_in = Seq("text")
  )
}
