package models.behaviors.behaviorparameter

import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.MessageEvent
import play.api.Configuration
import play.api.cache.CacheApi
import services.DataService

case class BehaviorParameterContext(
                                     event: MessageEvent,
                                     maybeConversation: Option[Conversation],
                                     parameter: BehaviorParameter,
                                     cache: CacheApi,
                                     dataService: DataService,
                                     configuration: Configuration
                                   )
