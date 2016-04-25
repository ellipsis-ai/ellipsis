package models.bots

trait MessageContext {
  def sendMessage(text: String): Unit
}
