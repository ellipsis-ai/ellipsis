package models.behaviors.conversations

import models.behaviors.BotResult
import models.behaviors.conversations.conversation.Conversation
import services.DefaultServices
import models.behaviors.events.Event
import slick.dbio.DBIO

import scala.concurrent.{ExecutionContext, Future}

trait CollectionState {

  val name: String

  val services: DefaultServices
  val event: Event

  def isCompleteIn(conversation: Conversation)(implicit ec: ExecutionContext): Future[Boolean]
  def collectValueFrom(conversation: InvokeBehaviorConversation)(implicit ec: ExecutionContext): Future[Conversation]
  def promptResultForAction(conversation: Conversation, isReminding: Boolean)(implicit ec: ExecutionContext): DBIO[BotResult]

}
