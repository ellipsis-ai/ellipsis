package models.bots

trait MessageContext {
  def sendMessage(text: String): Unit
  val name: String
  def userIdForContext: String
}
