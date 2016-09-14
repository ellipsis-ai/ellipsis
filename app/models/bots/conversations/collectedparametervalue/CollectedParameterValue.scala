package models.bots.conversations.collectedparametervalue

import models.bots.behaviorparameter.BehaviorParameter
import models.bots.conversations.conversation.Conversation

case class CollectedParameterValue(parameter: BehaviorParameter, conversation: Conversation, valueString: String) {

  def toRaw: RawCollectedParameterValue = RawCollectedParameterValue(parameter.id, conversation.id, valueString)
}
