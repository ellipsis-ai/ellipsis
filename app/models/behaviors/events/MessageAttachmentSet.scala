package models.behaviors.events

trait MessageAttachmentSet {

  type T <: MessageAttachment

  val attachments: Seq[T]
}
