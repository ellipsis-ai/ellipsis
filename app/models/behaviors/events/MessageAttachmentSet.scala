package models.behaviors.events

trait MessageAttachmentSet {

  type T <: MessageAttachment

  def attachments: Seq[T]
}
