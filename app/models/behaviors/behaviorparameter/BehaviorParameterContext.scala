package models.behaviors.behaviorparameter

import models.behaviors.conversations.conversation.Conversation
import play.api.Configuration
import play.api.cache.CacheApi
import services.DataService
import services.slack.NewMessageEvent

case class BehaviorParameterContext(
                                     event: NewMessageEvent,
                                     maybeConversation: Option[Conversation],
                                     parameter: BehaviorParameter,
                                     cache: CacheApi,
                                     dataService: DataService,
                                     configuration: Configuration
                                   )
