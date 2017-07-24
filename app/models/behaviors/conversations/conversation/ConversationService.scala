package models.behaviors.conversations.conversation

import java.time.OffsetDateTime

import akka.actor.ActorSystem
import slick.dbio.DBIO

import scala.concurrent.Future

trait ConversationService {

  def saveAction(conversation: Conversation): DBIO[Conversation]

  def save(conversation: Conversation): Future[Conversation]

  def allOngoingForAction(userIdForContext: String, context: String, maybeChannel: Option[String], maybeThreadId: Option[String]): DBIO[Seq[Conversation]]

  def allOngoingFor(userIdForContext: String, context: String, maybeChannel: Option[String], maybeThreadId: Option[String]): Future[Seq[Conversation]]

  def allForeground: Future[Seq[Conversation]]

  def maybeNextNeedingReminderAction(when: OffsetDateTime): DBIO[Option[Conversation]]

  def findOngoingFor(userIdForContext: String, context: String, maybeChannel: Option[String], maybeThreadId: Option[String]): Future[Option[Conversation]]

  def cancel(conversation: Conversation): Future[Unit]

  def cancel(maybeConversation: Option[Conversation]): Future[Unit] = {
    maybeConversation.map(cancel).getOrElse(Future.successful({}))
  }

  def deleteAll(): Future[Unit]

  def find(id: String): Future[Option[Conversation]]

  def isDone(id: String): Future[Boolean]

  def touchAction(conversation: Conversation): DBIO[Conversation]

  def touch(conversation: Conversation): Future[Conversation]

  def backgroundAction(conversation: Conversation, prompt: String, includeUsername: Boolean)(implicit actorSystem: ActorSystem): DBIO[Unit]

  def background(conversation: Conversation, prompt: String, includeUsername: Boolean)(implicit actorSystem: ActorSystem): Future[Unit]

}
