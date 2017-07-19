package models.behaviors.conversations

import models.behaviors.BotResult
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.Event
import play.api.Configuration
import services.{CacheService, DataService}

import scala.concurrent.Future

trait CollectionState {

  val name: String

  val event: Event
  val dataService: DataService
  val cacheService: CacheService
  val configuration: Configuration

  def isCompleteIn(conversation: Conversation): Future[Boolean]
  def collectValueFrom(conversation: InvokeBehaviorConversation): Future[Conversation]
  def promptResultFor(conversation: Conversation, isReminding: Boolean): Future[BotResult]

}
