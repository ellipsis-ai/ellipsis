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

  def isCompleteIn(conversation: Conversation)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Boolean]
  def collectValueFrom(conversation: InvokeBehaviorConversation)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Conversation]
  def promptResultForAction(conversation: Conversation, isReminding: Boolean)(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[BotResult]

}
