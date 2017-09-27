package models.behaviors.events

trait SlackMessageAttachmentSet extends MessageAttachmentSet {
  override type T = SlackMessageAttachment
}
