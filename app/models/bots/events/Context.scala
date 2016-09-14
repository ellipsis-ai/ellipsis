package models.bots.events

import models.bots.conversations.conversation.Conversation
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

trait Context {

  def sendMessage(text: String, forcePrivate: Boolean = false, maybeShouldUnfurl: Option[Boolean] = None)(implicit ec: ExecutionContext): Unit

  def maybeOngoingConversation(dataService: DataService): Future[Option[Conversation]]

}
