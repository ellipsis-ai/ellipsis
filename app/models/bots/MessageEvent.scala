package models.bots

trait MessageEvent extends Event {
  val context: MessageContext
}
