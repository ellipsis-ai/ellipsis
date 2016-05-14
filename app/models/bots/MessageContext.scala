package models.bots

import scala.concurrent.ExecutionContext

trait MessageContext {
  def sendMessage(text: String)(implicit ec: ExecutionContext): Unit
  val name: String
  def userIdForContext: String
}
