package models.behaviors.conversations.parentconversation

import models.behaviors.behaviorparameter.BehaviorParameter
import models.behaviors.conversations.conversation.Conversation

case class ParentConversation(id: String, parent: Conversation, param: BehaviorParameter) {

  def toRaw: RawParentConversation = RawParentConversation(id, parent.id, param.id)
}
