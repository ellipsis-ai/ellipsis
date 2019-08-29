package models.behaviors.conversations.collectedparametervalue

import models.behaviors.behaviorparameter.BehaviorParameter
import models.behaviors.conversations.conversation.Conversation
import slick.dbio.DBIO

import scala.concurrent.Future

trait CollectedParameterValueService {

  def allForAction(conversation: Conversation): DBIO[Seq[CollectedParameterValue]]

  def ensureForAction(parameter: BehaviorParameter, conversation: Conversation, valueString: String): DBIO[CollectedParameterValue]

  def ensureFor(parameter: BehaviorParameter, conversation: Conversation, valueString: String): Future[CollectedParameterValue]

  def deleteForAction(parameterId: String, conversation: Conversation): DBIO[Unit]

  def deleteFor(parameterId: String, conversation: Conversation): Future[Unit]

  def deleteAll(): Future[Unit]

}
