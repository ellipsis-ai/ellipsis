package models.behaviors.conversations.parentconversation

import models.behaviors.conversations.conversation.Conversation
import slick.dbio.DBIO

import scala.concurrent.{ExecutionContext, Future}

trait ParentConversationService {

  def createAction(pc: NewParentConversation): DBIO[ParentConversation]

  def ancestorsForAction(conversation: Conversation)(implicit ec: ExecutionContext): DBIO[List[Conversation]]

  def ancestorsFor(conversation: Conversation)(implicit ec: ExecutionContext): Future[List[Conversation]]

  def maybeForAction(conversation: Conversation): DBIO[Option[ParentConversation]]

  def maybeForAction(maybeConversation: Option[Conversation]): DBIO[Option[ParentConversation]] = {
    maybeConversation.map(maybeForAction).getOrElse(DBIO.successful(None))
  }

  def rootForAction(conversation: Conversation): DBIO[Conversation]

}
