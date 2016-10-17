package models.behaviors.testing

import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.MessageContext
import services.DataService

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future}

case class TestMessageContext(fullMessageText: String, includesBotMention: Boolean) extends MessageContext {

  val messageBuffer: ArrayBuffer[String] = new ArrayBuffer()
  override def relevantMessageText: String = fullMessageText
  val userIdForContext = "test"
  val name = "test"
  val teamId = "test"
  val isResponseExpected = true

  def maybeOngoingConversation(dataService: DataService): Future[Option[Conversation]] = Future.successful(None)

  def sendMessage(text: String, forcePrivate: Boolean = false, maybeShouldUnfurl: Option[Boolean] = None)(implicit ec: ExecutionContext): Unit = {
    messageBuffer += text
  }

}
