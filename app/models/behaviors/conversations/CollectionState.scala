package models.behaviors.conversations

import models.behaviors.BotResult
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.Event
import play.api.Configuration
import play.api.cache.CacheApi
import services.DataService

import scala.concurrent.Future

trait CollectionState {

  val name: String

  val event: Event
  val dataService: DataService
  val cache: CacheApi
  val configuration: Configuration

  def isCompleteIn(conversation: Conversation): Future[Boolean]
  def collectValueFrom(conversation: InvokeBehaviorConversation): Future[Conversation]
  def promptResultFor(conversation: Conversation): Future[BotResult]

}
