package models.behaviors.conversations.parentconversation

import models.behaviors.behaviorparameter.BehaviorParameter
import models.behaviors.conversations.conversation.Conversation

case class NewParentConversation(parent: Conversation, param: BehaviorParameter)
