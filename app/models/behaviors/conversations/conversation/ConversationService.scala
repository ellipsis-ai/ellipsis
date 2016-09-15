package models.behaviors.conversations.conversation

import scala.concurrent.Future

trait ConversationService {

  def save(conversation: Conversation): Future[Conversation]

  def findOngoingFor(userIdForContext: String, context: String): Future[Option[Conversation]]

}
