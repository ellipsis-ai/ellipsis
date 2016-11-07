package models.behaviors.events

import models.behaviors.conversations.conversation.Conversation
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

trait Context {

  def sendMessage(
                   text: String,
                   forcePrivate: Boolean,
                   maybeShouldUnfurl: Option[Boolean],
                   maybeConversation: Option[Conversation]
                 )(implicit ec: ExecutionContext): Unit

  def maybeOngoingConversation(dataService: DataService): Future[Option[Conversation]]

}
