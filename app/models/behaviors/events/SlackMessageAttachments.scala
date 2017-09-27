package models.behaviors.events

trait SlackMessageAttachments extends MessageAttachments {
  override type T = SlackMessageAttachment
}
