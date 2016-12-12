package models.behaviors.conversations.collectedparametervalue

import models.behaviors.behaviorparameter.BehaviorParameter
import models.behaviors.conversations.conversation.Conversation

import scala.concurrent.Future

trait CollectedParameterValueService {

  def allFor(conversation: Conversation): Future[Seq[CollectedParameterValue]]

  def ensureFor(parameter: BehaviorParameter, conversation: Conversation, valueString: String): Future[CollectedParameterValue]

  def deleteAll(): Future[Unit]

}
