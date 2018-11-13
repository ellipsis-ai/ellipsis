package models.behaviors.events.slack

import models.behaviors.events.MessageAttachmentGroup

trait SlackMessageAttachmentGroup extends MessageAttachmentGroup {
  type T = SlackMessageAttachment
}
