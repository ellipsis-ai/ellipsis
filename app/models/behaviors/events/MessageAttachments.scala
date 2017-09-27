package models.behaviors.events

trait MessageAttachments {

  type T <: MessageAttachment

  def attachments: Seq[T]
}
