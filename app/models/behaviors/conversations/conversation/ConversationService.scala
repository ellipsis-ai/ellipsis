package models.behaviors.conversations.conversation

import akka.actor.ActorSystem

import scala.concurrent.Future

trait ConversationService {

  def save(conversation: Conversation): Future[Conversation]

  def allOngoingFor(userIdForContext: String, context: String, maybeChannel: Option[String], maybeThreadId: Option[String]): Future[Seq[Conversation]]

  def allForeground: Future[Seq[Conversation]]

  def allNeedingReminder: Future[Seq[Conversation]]

  def findOngoingFor(userIdForContext: String, context: String, maybeChannel: Option[String], maybeThreadId: Option[String]): Future[Option[Conversation]]

  def cancel(conversation: Conversation): Future[Unit]

  def cancel(maybeConversation: Option[Conversation]): Future[Unit] = {
    maybeConversation.map(cancel).getOrElse(Future.successful({}))
  }

  def deleteAll(): Future[Unit]

  def find(id: String): Future[Option[Conversation]]

  def isDone(id: String): Future[Boolean]

  def touch(conversation: Conversation): Future[Conversation]

  def background(conversation: Conversation, prompt: String, includeUsername: Boolean)(implicit actorSystem: ActorSystem): Future[Unit]

}
