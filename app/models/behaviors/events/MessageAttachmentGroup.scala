package models.behaviors.events

trait MessageAttachmentGroup {

  type T <: MessageAttachment

  def attachments: Seq[T]
}
