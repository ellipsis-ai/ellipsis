package models.behaviors.conversations.collectedparametervalue

import models.behaviors.behaviorparameter.BehaviorParameter
import models.behaviors.conversations.conversation.Conversation
import slick.dbio.DBIO

import scala.concurrent.Future

trait CollectedParameterValueService {

  def allForAction(conversation: Conversation): DBIO[Seq[CollectedParameterValue]]

  def ensureFor(parameter: BehaviorParameter, conversation: Conversation, valueString: String): Future[CollectedParameterValue]

  def deleteAll(): Future[Unit]

}
