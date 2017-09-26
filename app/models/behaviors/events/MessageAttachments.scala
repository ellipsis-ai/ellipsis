package models.behaviors.events

trait MessageAttachments {

  type T <: MessageAttachment

  val attachments: Seq[T]
}
