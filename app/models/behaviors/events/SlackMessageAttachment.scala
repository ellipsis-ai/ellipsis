package models.behaviors.events

import models.SlackMessageFormatter
import slack.models.{ActionField, Attachment}

case class SlackMessageAttachment(
                                   maybeText: Option[String],
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
    text = maybeText.map(SlackMessageFormatter.bodyTextFor),
    fields = Seq(),
    image_url = None,
    thumb_url = None,
    actions = actions,
    mrkdwn_in = Seq("text")
  )
}
