package models.behaviors.conversations.conversation

import scala.concurrent.Future

trait ConversationService {

  def save(conversation: Conversation): Future[Conversation]

  def allOngoingFor(userIdForContext: String, context: String, isPrivateMessage: Boolean): Future[Seq[Conversation]]

  def findOngoingFor(userIdForContext: String, context: String, isPrivateMessage: Boolean): Future[Option[Conversation]]

  def find(id: String): Future[Option[Conversation]]

  def cancel(conversation: Conversation): Future[Unit]

  def cancel(maybeConversation: Option[Conversation]): Future[Unit] = {
    maybeConversation.map(cancel).getOrElse(Future.successful({}))
  }

  def deleteAll(): Future[Unit]

}
