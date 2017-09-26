package models.behaviors.events

import slack.models.Attachment

trait SlackMessageAttachments extends MessageAttachments {
  override val attachments: Seq[Attachment]
}
