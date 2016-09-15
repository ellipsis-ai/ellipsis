package models.behaviors.events

trait MessageEvent extends Event {
  val context: MessageContext
}
