package models.behaviors.conversations.conversation

import java.time.OffsetDateTime

import akka.actor.ActorSystem
import models.behaviors.BotResult
import models.behaviors.events.{Event, EventContext}
import slick.dbio.DBIO

import scala.concurrent.{ExecutionContext, Future}

trait ConversationService {

  def saveAction(conversation: Conversation): DBIO[Conversation]

  def save(conversation: Conversation): Future[Conversation]

  def maybeWithThreadId(threadId: String, userIdForContext: String, context: String): Future[Option[Conversation]]

  def allOngoingForAction(eventContext: EventContext, maybeBotResult: Option[BotResult])(implicit ec: ExecutionContext, actorSystem: ActorSystem): DBIO[Seq[Conversation]]

  def allOngoingFor(eventContext: EventContext, maybeBotResult: Option[BotResult])(implicit ec: ExecutionContext, actorSystem: ActorSystem): Future[Seq[Conversation]]

  def allOngoingBehaviorGroupVersionIds: Future[Seq[String]]

  def allForeground: Future[Seq[Conversation]]

  def maybeNextNeedingReminderAction(when: OffsetDateTime): DBIO[Option[Conversation]]

  def findOngoingForAction(userIdForContext: String, context: String, maybeChannel: Option[String], maybeThreadId: Option[String], teamId: String): DBIO[Option[Conversation]]

  def findOngoingFor(userIdForContext: String, context: String, maybeChannel: Option[String], maybeThreadId: Option[String], teamId: String): Future[Option[Conversation]]

  def cancelAction(conversation: Conversation): DBIO[Unit]

  def cancelAction(maybeConversation: Option[Conversation]): DBIO[Unit] = {
    maybeConversation.map(cancelAction).getOrElse(DBIO.successful({}))
  }

  def cancel(conversation: Conversation): Future[Unit]

  def cancel(maybeConversation: Option[Conversation]): Future[Unit] = {
    maybeConversation.map(cancel).getOrElse(Future.successful({}))
  }

  def cancelOldConverations: Future[Unit]

  def deleteAll(): Future[Unit]

  def find(id: String): Future[Option[Conversation]]

  def isDone(id: String): Future[Boolean]

  def touchAction(conversation: Conversation): DBIO[Conversation]

  def touch(conversation: Conversation): Future[Conversation]

  def interruptionPromptFor(event: Event, prompt: String, includeUsername: Boolean): String = {
    val usernameString = if (includeUsername) { event.messageRecipientPrefix } else { "" }
    s"""$usernameString$prompt You can continue the previous conversation in this thread:""".stripMargin
  }

  def backgroundAction(conversation: Conversation, prompt: String, includeUsername: Boolean)(implicit actorSystem: ActorSystem): DBIO[Unit]

  def background(conversation: Conversation, prompt: String, includeUsername: Boolean)(implicit actorSystem: ActorSystem): Future[Unit]

}
