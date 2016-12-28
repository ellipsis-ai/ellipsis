package models.behaviors.behaviorparameter

import models.behaviors.conversations.conversation.Conversation
import play.api.Configuration
import play.api.cache.CacheApi
import services.DataService
import services.slack.MessageEvent

case class BehaviorParameterContext(
                                     event: MessageEvent,
                                     maybeConversation: Option[Conversation],
                                     parameter: BehaviorParameter,
                                     cache: CacheApi,
                                     dataService: DataService,
                                     configuration: Configuration
                                   )
