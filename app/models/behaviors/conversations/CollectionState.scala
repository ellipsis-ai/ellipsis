package models.behaviors.conversations

import akka.actor.ActorSystem
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

  def isCompleteInAction(conversation: Conversation)(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[Boolean]
  def collectValueFromAction(conversation: InvokeBehaviorConversation)(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[Conversation]
  def promptResultForAction(conversation: Conversation, isReminding: Boolean)(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[BotResult]

}
