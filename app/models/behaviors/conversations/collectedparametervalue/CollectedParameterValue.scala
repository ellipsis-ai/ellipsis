package models.behaviors.conversations.collectedparametervalue

import models.behaviors.behaviorparameter.BehaviorParameter
import models.behaviors.conversations.conversation.Conversation

case class CollectedParameterValue(
                                    parameter: BehaviorParameter,
                                    conversation: Conversation,
                                    valueString: String
                                  ) {

  def toRaw: RawCollectedParameterValue = RawCollectedParameterValue(parameter.id, conversation.id, valueString)
}
