package models.behaviors.conversations

import models.behaviors.BotResult
import models.behaviors.conversations.conversation.Conversation
import services.DefaultServices

import scala.concurrent.Future

trait CollectionState {

  val name: String

  val services: DefaultServices

  def isCompleteIn(conversation: Conversation): Future[Boolean]
  def collectValueFrom(conversation: InvokeBehaviorConversation): Future[Conversation]
  def promptResultFor(conversation: Conversation, isReminding: Boolean): Future[BotResult]

}
