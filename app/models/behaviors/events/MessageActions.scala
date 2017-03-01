package models.behaviors.events

trait MessageActions {

  type T <: MessageAction

  val actions: Seq[T]

}
