package models.behaviors.events

import json.SlackUserData
import models.SlackMessageFormatter
import services.slack.apiModels.{ActionField, Attachment}

case class SlackMessageAttachment(
                                   maybeText: Option[String],
                                   maybeSlackUserList: Option[Set[SlackUserData]],
                                   maybeTitle: Option[String] = None,
                                   maybeTitleLink: Option[String] = None,
                                   maybeColor: Option[String] = None,
                                   maybeCallbackId: Option[String] = None,
                                   actions: Seq[ActionField] = Seq()
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
    text = maybeText.map(text => SlackMessageFormatter.bodyTextFor(text, maybeSlackUserList.getOrElse(Set.empty[SlackUserData]))),
    fields = Seq(),
    image_url = None,
    thumb_url = None,
    actions = actions,
    mrkdwn_in = Seq("text")
  )
}
