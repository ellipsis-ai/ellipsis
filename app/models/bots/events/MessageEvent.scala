package models.bots.events

trait MessageEvent extends Event {
  val context: MessageContext
}
